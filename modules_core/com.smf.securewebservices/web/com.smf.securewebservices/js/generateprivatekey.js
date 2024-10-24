isc.ClassFactory.defineClass('SMFSWSGenerateKeyButton', isc.OBFormButton);

isc.SMFSWSGenerateKeyButton.addProperties({
    title: OB.I18N.getLabel('SMFSWS_GenerateKeys'),
    width: 200,
    click: async function() {
        try {
            let form = this.canvasItem.form;
            let algorithmField = form.getFieldFromFieldName('algorithm');
            let selectedAlgorithm = algorithmField?.getValue();
			// Generate ES256 keys by default if no algorithm is selected.
            if (selectedAlgorithm === 'ES256' || !selectedAlgorithm) {
                const keyPair = await window.crypto.subtle.generateKey(
                    {
                        name: "ECDSA",
                        namedCurve: "P-256", // ES256 uses the P-256 curve
                    },
                    true,
                    ["sign", "verify"]
                );

                const privateKey = await window.crypto.subtle.exportKey("pkcs8", keyPair.privateKey);
                const privateKeyPem = arrayBufferToPem(privateKey, "PRIVATE KEY");

                const publicKey = await window.crypto.subtle.exportKey("spki", keyPair.publicKey);
                const publicKeyPem = arrayBufferToPem(publicKey, "PUBLIC KEY");
                updateKeyFields(form, privateKeyPem, publicKeyPem);
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
                updateKeyFields(form, secretKeyPem, ''); // Only update the private key field
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

// Update the private and public key fields with the generated keys
function updateKeyFields(form, privateKeyValue, publicKeyValue) {
    let privateKeyField = form.getFieldFromFieldName('privateKey');
    let publicKeyField = form.getFieldFromFieldName('publicKey');
    privateKeyField.setValue(privateKeyValue);
    publicKeyField.setValue(publicKeyValue);
    privateKeyField.handleChanged();
    publicKeyField.handleChanged();
}
