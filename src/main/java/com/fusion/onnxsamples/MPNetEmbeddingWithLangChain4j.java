package com.fusion.onnxsamples;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.store.embedding.CosineSimilarity;
public class MPNetEmbeddingWithLangChain4j {
    public static void main(String[] args) throws Exception {
        EmbeddingModel embeddingModel = new OnnxEmbeddingModel(
                "/Users/satyaanumolu/POCs/RAGExamples/src/main/resources/onnx/all-mpnet-base-v2.onnx",
                "/Users/satyaanumolu/POCs/RAGExamples/src/main/resources/onnx/all-mpnet-base-v2-tokenizer.json",
                PoolingMode.MEAN
        );
        String englishText = "Hello, how are you doing?";
        String frenchText = "Bonjour comment allez-vous?";

        String englishText1 = "Doing Good?";

        Embedding englishTextEmbedding = embeddingModel.embed(englishText).content();
        Embedding english1TextEmbedding = embeddingModel.embed(englishText1).content();
        Embedding frenchTextEmbedding = embeddingModel.embed(frenchText).content();

        System.out.println("English and French : " + CosineSimilarity.between(englishTextEmbedding, frenchTextEmbedding));
        System.out.println("\n");
        System.out.println("English Only :: " + CosineSimilarity.between(englishTextEmbedding, english1TextEmbedding));


    }
}