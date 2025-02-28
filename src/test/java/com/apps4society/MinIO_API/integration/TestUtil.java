package com.apps4society.MinIO_API.integration;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TestUtil {

    private static final String MINIO_URL = "http://127.0.0.1:9000";
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "test-bucket";

    private static final String JDBC_URL = "jdbc:h2:mem:testdb";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    private static final MinioClient minioClient = MinioClient.builder()
            .endpoint(MINIO_URL)
            .credentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
            .build();

    /**
     * 📌 Adiciona um arquivo ao MinIO (simulando um upload)
     */
    public static void createTestMedia(String fileName) {
        try {
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
     * 📌 Adiciona um registro ao banco H2
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

    /**
     * 📌 Remove um arquivo do MinIO
     */
    public static void deleteFileFromMinIO(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .build()
            );

            System.out.println("✅ Arquivo removido do MinIO: " + fileName);

        } catch (Exception e) {
            System.err.println("❌ Erro ao deletar arquivo do MinIO: " + e.getMessage());
        }
    }

    /**
     * 📌 Remove um registro do banco H2 com base no nome do arquivo
     */
    public static void deleteRecordFromDatabase(String fileName) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            String sql = "DELETE FROM media WHERE file_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileName);
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    System.out.println("✅ Registro removido do banco: " + fileName);
                } else {
                    System.out.println("⚠ Nenhum registro encontrado para: " + fileName);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao deletar do banco H2: " + e.getMessage());
        }
    }

    /**
     * 📌 Método geral para limpeza de mídia após os testes
     */
    public static void cleanupTestData(String fileName) {
        deleteFileFromMinIO(fileName);
        deleteRecordFromDatabase(fileName);
    }
}
