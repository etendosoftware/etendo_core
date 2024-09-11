isc.ClassFactory.defineClass('SMFSWSGenerateKeyButton', isc.OBFormButton);

isc.SMFSWSGenerateKeyButton.addProperties({
    title: OB.I18N.getLabel('SMFSWS_GenerateKeys'),
    width: 200,
    click: async function() {
        try {
            let form = this.canvasItem.form;
            let algorithmField = form.getFieldFromFieldName('algorithm');
            let selectedAlgorithm = algorithmField.getValue();

            if (selectedAlgorithm === 'RS256') {
                // Generate secret keys for RSA
                const keyPair = await window.crypto.subtle.generateKey(
                    {
                        name: "RSASSA-PKCS1-v1_5",
                        modulusLength: 2048,
                        publicExponent: new Uint8Array([1, 0, 1]),
                        hash: "SHA-256",
                    },
                    true,
                    ["sign", "verify"]
                );

                const privateKey = await window.crypto.subtle.exportKey("pkcs8", keyPair.privateKey);
                const privateKeyPem = arrayBufferToPem(privateKey, "PRIVATE KEY");

                const publicKey = await window.crypto.subtle.exportKey("spki", keyPair.publicKey);
                const publicKeyPem = arrayBufferToPem(publicKey, "PUBLIC KEY");

                let privateKeyField = form.getFieldFromFieldName('privateKey');
                let publicKeyField = form.getFieldFromFieldName('publicKey');
                privateKeyField.setValue(privateKeyPem);
                publicKeyField.setValue(publicKeyPem);
                privateKeyField.handleChanged();
                publicKeyField.handleChanged();
            } else if (selectedAlgorithm === 'HS256') {
                // Generate secret key for HMAC (HS256)
                const key = await window.crypto.subtle.generateKey(
                    {
                        name: "HMAC",
                        hash: { name: "SHA-256" },
                        length: 256
                    },
                    true,
                    ["sign", "verify"]
                );

                const secretKey = await window.crypto.subtle.exportKey("raw", key);
                const secretKeyPem = arrayBufferToPem(secretKey, "SECRET KEY");

                let privateKeyField = form.getFieldFromFieldName('privateKey');
                let publicKeyField = form.getFieldFromFieldName('publicKey');
                privateKeyField.setValue(secretKeyPem);
                publicKeyField.setValue('');
                privateKeyField.handleChanged();
                publicKeyField.handleChanged();
            }
        } catch (error) {
            console.error("Error generating keys:", error);
        }
    }
});

// Convert ArrayBuffer to PEM format
function arrayBufferToPem(buffer, label) {
    const binary = String.fromCharCode(...new Uint8Array(buffer));
    const base64 = window.btoa(binary);
    const pem = `-----BEGIN ${label}-----\n${base64.match(/.{1,64}/g).join('\n')}\n-----END ${label}-----\n`;
    return pem;
}
