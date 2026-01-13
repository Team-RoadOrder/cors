from fastapi import FastAPI
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import linear_kernel, cosine_similarity
from sqlalchemy import create_engine
import numpy as np

app = FastAPI()

# ==========================================
# 1. DB 연결 설정
# ==========================================
DB_USER = "root"
DB_PASS = "test1234"
DB_HOST = "localhost"
DB_PORT = "33061"
DB_NAME = "cors"

db_connection_str = f'mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}'
db_connection = create_engine(db_connection_str)

# 전역 변수
final_sim_matrix = None
indices = None
data = None

def load_data_and_train_model():
    global final_sim_matrix, indices, data
    print("🔄 [AI] Hybrid 모델 학습 시작 (Contents + Collaborative)...")

    try:
        # ---------------------------------------------------------
        # A. 상품 데이터 로드 (Content-based)
        # ---------------------------------------------------------
        query_items = """
                      SELECT id, item_name, main_category, sub_category, style
                      FROM shop_items
                      WHERE deleted_at IS NULL \
                      """
        data = pd.read_sql(query_items, db_connection)

        if data.empty:
            print("⚠ 상품 데이터가 없습니다.")
            return

        # 텍스트 합치기
        data['combined_features'] = (
                data['item_name'].fillna('') + " " +
                data['main_category'].fillna('') + " " +
                data['sub_category'].fillna('') + " " +
                data['style'].fillna('')
        )

        # TF-IDF 벡터화 (콘텐츠 유사도)
        tfidf = TfidfVectorizer(stop_words='english')
        tfidf_matrix = tfidf.fit_transform(data['combined_features'])
        content_sim = linear_kernel(tfidf_matrix, tfidf_matrix)

        print("✅ [1/2] 콘텐츠 기반 유사도 계산 완료")

        # ---------------------------------------------------------
        # B. 좋아요 데이터 로드 (Collaborative Filtering)
        # ---------------------------------------------------------
    # 테이블명: like_items (복수형), 컬럼명: user_email (Java Entity 기준)
        query_likes = "SELECT user_email as user_id, item_id FROM like_items"
        likes_data = pd.read_sql(query_likes, db_connection)

        collab_sim = np.zeros(content_sim.shape) # 기본은 0으로 초기화

        if not likes_data.empty:
            # 1. Pivot Table 생성 (행: Item, 열: User, 값: 1)
            # 데이터가 있는 상품만 추리기 위해 merge 사용
            item_user_matrix = data[['id']].merge(likes_data, left_on='id', right_on='item_id', how='left')

            # 좋아요가 하나도 없는 상품은 user_id가 NaN이므로 0으로 채움, pivot을 위해 더미 컬럼 활용 가능하지만
            # 간단하게 crosstab 사용 (User가 Item을 좋아했으면 1, 아니면 0)
            user_item_pivot = pd.crosstab(item_user_matrix['id'], item_user_matrix['user_id'])

            # 2. 아이템 간 코사인 유사도 계산 (사람들이 같이 좋아한 패턴)
            # DB에 있는 모든 상품 ID 순서를 맞춰야 하므로 reindex 필요할 수 있음.
            # 하지만 여기서는 간단하게 shape가 맞는지 확인하고 진행.

            # pivot 테이블의 인덱스(item_id)가 data['id'] 순서와 일치하도록 정렬
            user_item_pivot = user_item_pivot.reindex(data['id'], fill_value=0)

            collab_sim = cosine_similarity(user_item_pivot)
            print(f"✅ [2/2] 협업 필터링(좋아요 {len(likes_data)}건) 반영 완료")
        else:
            print("⚠ [Notice] 좋아요 데이터가 없어 콘텐츠 기반으로만 동작합니다.")

        # ---------------------------------------------------------
        # C. 하이브리드 결합 (Weighted Hybrid)
        # ---------------------------------------------------------
        # 가중치: 콘텐츠 70% + 협업 30% (좋아요 데이터가 적을 때 유리)
        # 데이터가 쌓이면 협업 비중(0.3)을 늘리면 됨.
        w_content = 0.7
        w_collab = 0.3

        final_sim_matrix = (w_content * content_sim) + (w_collab * collab_sim)

        # ID -> Index 매핑
        indices = pd.Series(data.index, index=data['id']).drop_duplicates()

        print("🎉 [AI] 하이브리드 모델 로딩 완료!")

    except Exception as e:
        print(f"❌ DB 연결 또는 학습 실패: {e}")

@app.on_event("startup")
def startup_event():
    load_data_and_train_model()

@app.get("/recommend/{item_id}")
def recommend(item_id: int):
    # 데이터 체크
    if data is None or indices is None or final_sim_matrix is None:
        return {"recommendations": []}

    if item_id not in indices:
        return {"recommendations": []}

    # 1. 인덱스 찾기
    idx = indices[item_id]

    # 2. 하이브리드 유사도 점수 가져오기
    sim_scores = list(enumerate(final_sim_matrix[idx]))

    # 3. 점수 높은 순 정렬
    sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)

    # 4. 상위 4개 (자기 자신 제외)
    sim_scores = sim_scores[1:4]

    # 5. 결과 반환
    item_indices = [i[0] for i in sim_scores]
    recommend_items = data.iloc[item_indices][['id']].to_dict(orient='records')

    return {"recommendations": recommend_items}