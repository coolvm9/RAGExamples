package com.fusion.onnxsamples;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.store.embedding.CosineSimilarity;
public class MPNetEmbeddingWithLangChain4j {
    public static void main(String[] args) throws Exception {
        // Path to your ONNX model (adjust the path according to your directory)
        EmbeddingModel embeddingModel = new OnnxEmbeddingModel(
                "/Users/satyaanumolu/POCs/RAGExamples/src/main/resources/onnx/all-mpnet-base-v2.onnx",
                "/Users/satyaanumolu/POCs/RAGExamples/src/main/resources/onnx/all-mpnet-base-v2-tokenizer.json",
                PoolingMode.MEAN
        );
        String englishText = "Hello, how are you doing?";
        String frenchText = "Bonjour comment allez-vous?";

        Embedding englishTextEmbedding = embeddingModel.embed(englishText).content();
        Embedding frenchTextEmbedding = embeddingModel.embed(frenchText).content();

        System.out.println(CosineSimilarity.between(englishTextEmbedding, frenchTextEmbedding));


    }
}