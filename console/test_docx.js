const { Document, Packer, Paragraph, TextRun } = require('docx');
const fs = require('fs');

console.log('Starting...');

const doc = new Document({
    sections: [{
        children: [
            new Paragraph({
                children: [new TextRun({ text: "Hello World", size: 48 })]
            })
        ]
    }]
});

console.log('Document created, packing...');

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("test_output.docx", buffer);
    console.log("File saved successfully!");
}).catch(err => {
    console.error("Error:", err);
});
