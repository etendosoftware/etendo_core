from . import exceptions, constants, repo_api_requests
from . import *


extension_white_list = [
    "java",
    "js",
    "jsx",
    "ts",
    "tsx",
    "py",
    "css",
    "gradle",
    "Jenkinsfile",
]
openai_api_key = os.getenv("OPENAI_API_KEY")


def send_gpt_review(diffs, pull_request, repo_slug, is_bitbucket):
    gpt_reviews = []

    for diff in diffs:
        file = get_metadata(diff)
        if (
            file["extension"] not in extension_white_list
            or file["action"] == "deleted"
            or file["percentage"] == "100%"
        ):
            continue  # Skip deleted or moved/renamed but untouched files

        gpt_review = get_gpt_review(file["diff"])
        repo_api_requests.comment_file(
            pull_request,
            repo_slug,
            is_bitbucket,
            file=file["file_path"],
            review="# GPT Review for " + file["file_name"] + "\n" + gpt_review,
        )
        gpt_reviews.append((file["file_name"], gpt_review))

    return gpt_reviews


def get_gpt_review(diff):
    openai.api_key = openai_api_key

    with open("app/prompt.txt") as prompt:
        system_prompt = prompt.read()
    diff_tokens = len(diff) / 4
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": "GIT DIFF: \n" + diff},
    ]
    if diff_tokens < constants.DIFF_MAX_SIZE:  # 1 token ~ 4 chars
        # Send the request to the OpenAI API
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=messages,
            temperature=constants.TEMPERATURE,
            max_tokens=constants.MAX_TOKENS_REVIEW,
            top_p=1,
            frequency_penalty=0,
            presence_penalty=0,
        )

        return response.choices[0]["message"]["content"]


def get_metadata(f):
    lines = f.split("\n")
    file_name = ""
    extension = ""
    for index, line in enumerate(lines):
        if line.startswith("diff --git"):
            _, _, file_path_1, file_path_2 = line.split(" ")
            snd_line_data = lines[index + 1].split(" ")
            try:
                action = snd_line_data[0]
                percentage = snd_line_data[2]
                # Get the filename by splitting the path and taking the last element
                file_name = file_path_1.split("/")[-1]
                # Get the extension by splitting the filename by "." and taking the last element
                extension = file_name.rsplit(".", 1)[-1]
            except (IndexError, ValueError) as e:
                raise exceptions.MetadataParsingError(
                    "Error parsing metadata from diff file: {}".format(e)
                )
            break
    return {
        "file_path": file_path_2[2:],  # Cut off 'b/' chars
        "file_name": file_name,
        "extension": extension,
        "diff": f,
        "action": action,
        "percentage": percentage,
    }
