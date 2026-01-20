package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.LikeItemEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final OwnerShopMapper ownerShopMapper;

    // Caches
    private List<ShopItemEntity> allItems;
    private Map<Long, Integer> itemIndexMap; // ItemID -> Index
    private double[][] finalSimMatrix;

    private static final double WEIGHT_CONTENT = 0.7;
    private static final double WEIGHT_COLLAB = 0.3;

    @PostConstruct
    public void init() {
        loadDataAndTrainModel();
    }

    /**
     * 데이터 로드 및 모델(유사도 매트릭스) 학습
     * 주기적으로 호출하거나 관리자 기능으로 호출하여 모델을 갱신할 수 있습니다.
     */
    public void loadDataAndTrainModel() {
        log.info("🔄 [AI] Hybrid 모델 학습 시작 (Java)...");
        try {
            // 1. Load Items
            ShopItemEntity[] itemsArray = ownerShopMapper.selectAll();
            if (itemsArray == null || itemsArray.length == 0) {
                log.warn("⚠ 상품 데이터가 없습니다.");
                return;
            }
            this.allItems = Arrays.asList(itemsArray);

            // Map ID to Index
            this.itemIndexMap = new HashMap<>();
            for (int i = 0; i < allItems.size(); i++) {
                itemIndexMap.put(allItems.get(i).getId(), i);
            }

            int nItems = allItems.size();

            // 2. Content-Based Filtering (TF-IDF)
            double[][] contentSim = calculateContentSimilarity(nItems);
            log.info("✅ [1/2] 콘텐츠 기반 유사도 계산 완료");

            // 3. Collaborative Filtering (User Likes)
            double[][] collabSim = calculateCollaborativeSimilarity(nItems);
            log.info("✅ [2/2] 협업 필터링 반영 완료");

            // 4. Hybrid Combination
            this.finalSimMatrix = new double[nItems][nItems];
            for (int i = 0; i < nItems; i++) {
                for (int j = 0; j < nItems; j++) {
                    finalSimMatrix[i][j] = (WEIGHT_CONTENT * contentSim[i][j]) + (WEIGHT_COLLAB * collabSim[i][j]);
                }
            }
            log.info("🎉 [AI] 하이브리드 모델 로딩 완료!");

        } catch (Exception e) {
            log.error("❌ 학습 실패: ", e);
        }
    }

    private double[][] calculateContentSimilarity(int nItems) {
        // Prepare documents
        List<List<String>> documents = new ArrayList<>();
        Set<String> vocabulary = new HashSet<>();

        for (ShopItemEntity item : allItems) {
            String text = (nvl(item.getItemName()) + " " +
                    nvl(item.getMainCategory()) + " " +
                    nvl(item.getSubCategory()) + " " +
                    nvl(item.getStyle())).toLowerCase();

            // Simple tokenizer (split by space)
            List<String> tokens = Arrays.asList(text.split("\\s+"));
            documents.add(tokens);
            vocabulary.addAll(tokens);
        }

        List<String> vocabList = new ArrayList<>(vocabulary);
        int vocabSize = vocabList.size();
        Map<String, Integer> vocabIndex = new HashMap<>();
        for (int i = 0; i < vocabSize; i++) {
            vocabIndex.put(vocabList.get(i), i);
        }

        // Calculate TF-IDF
        // 1. TF
        double[][] tf = new double[nItems][vocabSize];
        for (int i = 0; i < nItems; i++) {
            List<String> doc = documents.get(i);
            for (String token : doc) {
                if (vocabIndex.containsKey(token)) {
                    tf[i][vocabIndex.get(token)]++;
                }
            }
        }

        // 2. IDF
        double[] idf = new double[vocabSize];
        for (int j = 0; j < vocabSize; j++) {
            int docCount = 0;
            for (int i = 0; i < nItems; i++) {
                if (tf[i][j] > 0) docCount++;
            }
            // IDF = log((N + 1) / (df + 1)) + 1
            idf[j] = Math.log((double)(nItems + 1) / (docCount + 1)) + 1.0;
        }

        // 3. TF-IDF Vectors
        double[][] tfidf = new double[nItems][vocabSize];
        for (int i = 0; i < nItems; i++) {
            for (int j = 0; j < vocabSize; j++) {
                tfidf[i][j] = tf[i][j] * idf[j];
            }
            // L2 Normalize
            double norm = 0.0;
            for (int j = 0; j < vocabSize; j++) {
                norm += tfidf[i][j] * tfidf[i][j];
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int j = 0; j < vocabSize; j++) {
                    tfidf[i][j] /= norm;
                }
            }
        }

        // 4. Cosine Similarity
        return computeCosineSimilarity(tfidf);
    }

    private double[][] calculateCollaborativeSimilarity(int nItems) {
        LikeItemEntity[] likesArray = ownerShopMapper.selectAllLikeItems();
        if (likesArray == null || likesArray.length == 0) {
            return new double[nItems][nItems]; // Return zero matrix
        }
        List<LikeItemEntity> likes = Arrays.asList(likesArray);

        // Get unique users
        List<String> users = likes.stream()
                .map(LikeItemEntity::getUserEmail)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Integer> userIndex = new HashMap<>();
        for (int i = 0; i < users.size(); i++) {
            userIndex.put(users.get(i), i);
        }
        int nUsers = users.size();

        // Build Item-User Matrix (Rows: Items, Cols: Users)
        double[][] itemUserMatrix = new double[nItems][nUsers];

        for (LikeItemEntity like : likes) {
            Long itemId = like.getItemId();
            String userEmail = like.getUserEmail();

            if (itemIndexMap.containsKey(itemId) && userIndex.containsKey(userEmail)) {
                int i = itemIndexMap.get(itemId);
                int u = userIndex.get(userEmail);
                itemUserMatrix[i][u] = 1.0;
            }
        }

        // Cosine Similarity
        return computeCosineSimilarity(itemUserMatrix);
    }

    private double[][] computeCosineSimilarity(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] sim = new double[rows][rows];

        // Normalize rows
        double[][] normalized = new double[rows][cols];
        for(int i=0; i<rows; i++) {
            double norm = 0.0;
            for(int j=0; j<cols; j++) {
                norm += matrix[i][j] * matrix[i][j];
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for(int j=0; j<cols; j++) {
                    normalized[i][j] = matrix[i][j] / norm;
                }
            }
        }

        // Compute A . B
        for (int i = 0; i < rows; i++) {
            for (int j = i; j < rows; j++) { // Symmetric
                double dotProduct = 0.0;
                for (int k = 0; k < cols; k++) {
                    dotProduct += normalized[i][k] * normalized[j][k];
                }
                sim[i][j] = dotProduct;
                sim[j][i] = dotProduct;
            }
        }
        return sim;
    }

    public List<ShopItemEntity> getRecommendations(Long itemId) {
        if (finalSimMatrix == null || itemIndexMap == null || !itemIndexMap.containsKey(itemId)) {
            return Collections.emptyList();
        }

        int idx = itemIndexMap.get(itemId);
        double[] scores = finalSimMatrix[idx];

        // Pair of (Index, Score)
        List<Map.Entry<Integer, Double>> scoreList = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            if (i != idx) { // Exclude self
                scoreList.add(new AbstractMap.SimpleEntry<>(i, scores[i]));
            }
        }

        // Sort descending
        scoreList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Top 3
        return scoreList.stream()
                .limit(3)
                .map(e -> allItems.get(e.getKey()))
                .collect(Collectors.toList());
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
