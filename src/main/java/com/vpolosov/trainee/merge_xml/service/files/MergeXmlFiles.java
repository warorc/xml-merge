package com.vpolosov.trainee.merge_xml.service.files;

import com.vpolosov.trainee.merge_xml.aspect.Loggable;
import com.vpolosov.trainee.merge_xml.handler.exception.IncorrectXmlFileException;
import com.vpolosov.trainee.merge_xml.validators.XmlValidator;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class MergeXmlFiles {

    private static final Logger log = LoggerFactory.getLogger(MergeXmlFiles.class);
    private static final String HEADER_TAG= "BSHead";
    private static final String MESSAGE_TAG= "BSMessage";
    private static final String ID_TAG= "ID";
    private static final String DATE_TIME_TAG= "DateTime";
    private static final String DOCUMENTS_TAG= "DOCUMENTS";

    @Loggable
    public File merge(List<File> sourceXml, File xsdFile, String target) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        File targetFile = new File(sourceXml.get(0).getParentFile().getPath(), target);
        XmlValidator xmlValidator = new XmlValidator();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document targetDocument = documentBuilder.newDocument();

        for (File sourceFile : sourceXml) {
            if (!xmlValidator.isValid(xsdFile, sourceFile)) {
                log.error("Файл {} не прошел проверку.", sourceFile.getName());
                throw new IncorrectXmlFileException("Invalid XML file with name: " + sourceFile.getName());
            }

            log.info("Файл {} прошел проверку.", sourceFile.getName());

            Document document = documentBuilder.parse(sourceFile);
            document.getDocumentElement().normalize();

            if (targetDocument.getElementsByTagName(DOCUMENTS_TAG).getLength() == 0) {
                NodeList documentNodeList = document.getChildNodes();
                Node targetNode = targetDocument.importNode(documentNodeList.item(0), true);
                targetDocument.appendChild(targetNode);
            } else {
                NodeList headerNodeList = document.getElementsByTagName(HEADER_TAG).item(0).getChildNodes();

                for (int i = 0; i < headerNodeList.getLength(); i++) {
                    Node headerNode = headerNodeList.item(i);
                    Node targetHeaderNode = targetDocument.importNode(headerNode, true);

                    Element targetHeaderElement = (Element) targetDocument.getElementsByTagName(HEADER_TAG).item(0);
                    targetHeaderElement.appendChild(targetHeaderNode);
                }

                NodeList documentNodeList = document.getElementsByTagName(DOCUMENTS_TAG).item(0).getChildNodes();

                for (int i = 0; i < documentNodeList.getLength(); i++) {
                    Node documentNode = documentNodeList.item(i);
                    Node targetDocumentNode = targetDocument.importNode(documentNode, true);
                    targetDocument.normalize();
                    Element targetDocumentElement = (Element) targetDocument.getElementsByTagName(DOCUMENTS_TAG).item(0);
                    targetDocumentElement.appendChild(targetDocumentNode);
                }
            }
        }

        targetDocument.normalizeDocument();
        targetDocument.getElementsByTagName(MESSAGE_TAG).item(0).getAttributes().getNamedItem(ID_TAG)
                .setNodeValue(UUID.randomUUID().toString());
        targetDocument.getElementsByTagName(MESSAGE_TAG).item(0).getAttributes().getNamedItem(DATE_TIME_TAG)
                .setNodeValue(LocalDateTime.now().toString());


        targetDocument.normalize();
        DOMSource dom = new DOMSource(targetDocument);

        Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        Result result = new StreamResult(targetFile);
        transformer.transform(dom, result);
        return targetFile;
    }
}
