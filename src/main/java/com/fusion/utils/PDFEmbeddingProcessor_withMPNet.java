package com.fusion.utils;

import com.opencsv.CSVWriter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PDFEmbeddingProcessor_withMPNet {

    private static final Logger logger = LoggerFactory.getLogger(PDFEmbeddingProcessor_withMPNet.class);

    private final EmbeddingModel embeddingModel;

    public PDFEmbeddingProcessor_withMPNet() {

        this.embeddingModel = new OnnxEmbeddingModel(
                "/Users/satyaanumolu/POCs/RAGExamples/src/main/resources/onnx/all-mpnet-base-v2.onnx",
                "/Users/satyaanumolu/POCs/RAGExamples/src/main/resources/onnx/all-mpnet-base-v2-tokenizer.json",
                PoolingMode.MEAN
        );
    }

    public void processPdfAndWriteToCsv(String pdfPath, String csvOutputPath) throws Exception {
        Path path = Paths.get(pdfPath);
        Document document = FileSystemDocumentLoader.loadDocument(path, new ApachePdfBoxDocumentParser());

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = documentSplitter.split(document);
        // Write metadata, text, and embeddings to a CSV file
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvOutputPath))) {
            // Write the CSV header
            String[] header = {"Text Segment", "Embedding", "Document Metadata"};
            writer.writeNext(header);

            // Process each chunk (text segment)
            for (TextSegment segment : segments) {
                // Convert text segment into embeddings
                Response<Embedding> response = embeddingModel.embed(segment.text());

                segment.metadata().put("custom data" , "this is customdata");
                // Collect the necessary metadata
                String documentMetadata = segment.metadata().toString();


                // Convert embedding to a string (space-separated values)
                String embeddingString = response.content().vectorAsList().toString();

                // Write the text, embedding, and metadata to CSV
                String[] row = {
                        segment.text(),
                        embeddingString,
                        documentMetadata

                };
                writer.writeNext(row);
            }
        } catch (IOException e) {
            logger.error("Error writing to CSV", e);
            throw new Exception("Error writing to CSV file", e);
        }

        logger.info("Processed PDF and wrote embeddings to CSV: {}", csvOutputPath);
    }


    public static void main(String[] args) {
        try {
            // Load the PDF from resources/examples directory
            ClassLoader classLoader = PDFEmbeddingProcessor_withMPNet.class.getClassLoader();
            Path pdfPath = Paths.get(classLoader.getResource("example-files/2025_US_F150_Warranty_Guide_ENG_V1.pdf").toURI());  // Adjust the file name accordingly
            String csvOutputPath = "src/main/resources/output/embeddings_with_mpnet.csv";
            // Update this to where you want to save the CSV

            // Create an instance of the processor
            PDFEmbeddingProcessor_withMPNet processor = new PDFEmbeddingProcessor_withMPNet();

            // Process the PDF and write to CSV
            processor.processPdfAndWriteToCsv(pdfPath.toString(), csvOutputPath);
            logger.info("PDF processing and embedding generation completed successfully. CSV saved to: {}", csvOutputPath);
        } catch (URISyntaxException e) {
            logger.error("Error locating the PDF file in resources: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error processing the PDF: {}", e.getMessage(), e);
        }
    }
}