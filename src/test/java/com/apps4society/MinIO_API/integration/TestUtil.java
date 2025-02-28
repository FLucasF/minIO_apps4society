package com.apps4society.MinIO_API.integration;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TestSetupUtil {

    private static final String MINIO_URL = "http://127.0.0.1:9000";
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "test-bucket";

    private static final String JDBC_URL = "jdbc:h2:mem:testdb";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    /**
     * Adiciona um arquivo ao MinIO (simulando um upload)
     */
    public static void createTestMedia(String fileName) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(MINIO_URL)
                    .credentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
                    .build();

            byte[] content = "teste de upload".getBytes();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType("image/png")
                            .build()
            );

            System.out.println("✅ Arquivo adicionado ao MinIO: " + fileName);

            // Agora adicionamos ao banco de dados H2
            addMediaToDatabase(fileName);

        } catch (Exception e) {
            System.err.println("❌ Erro ao adicionar mídia no MinIO: " + e.getMessage());
        }
    }

    /**
     * Adiciona um registro ao banco H2
     */
    private static void addMediaToDatabase(String fileName) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            String sql = "INSERT INTO media (id, service_name, file_name, entity_type, media_type, uploaded_by, active, tag) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, 1);
                stmt.setString(2, "educaAPI");
                stmt.setString(3, fileName);
                stmt.setString(4, "THEME");
                stmt.setString(5, "IMAGE");
                stmt.setLong(6, 1);
                stmt.setBoolean(7, true);
                stmt.setString(8, "teste");

                stmt.executeUpdate();
                System.out.println("✅ Mídia inserida no banco: " + fileName);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir mídia no banco: " + e.getMessage());
        }
    }
}
