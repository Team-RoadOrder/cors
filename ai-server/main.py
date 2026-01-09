from fastapi import FastAPI
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import linear_kernel
from sqlalchemy import create_engine
import uvicorn
from contextlib import asynccontextmanager
import socket
import sys

# ==========================================
# 1. DB 연결 (application.properties 정보 반영)
# ==========================================
DB_USER = "root"
DB_PASS = "test1234"
DB_HOST = "localhost"
DB_PORT = "33061"
DB_NAME = "cors"

# SQLAlchemy 엔진 생성
db_connection_str = f'mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}'
db_connection = create_engine(db_connection_str)

# 전역 변수
tfidf_matrix = None
indices = None
data = None

def load_data_and_train_model():
    global tfidf_matrix, indices, data
    print("[AI] DB에서 상품 데이터를 학습 중입니다...")

    try:
        # 삭제되지 않은 상품만 조회
        query = """
                SELECT id, item_name, main_category, sub_category, style
                FROM shop_items
                WHERE deleted_at IS NULL \
                """
        data = pd.read_sql(query, db_connection)

        if data.empty:
            print("[AI] 데이터가 없습니다.")
            return

        # 텍스트 합치기 (이름 + 카테고리 + 스타일)
        data['combined_features'] = (
                data['item_name'].fillna('') + " " +
                data['main_category'].fillna('') + " " +
                data['sub_category'].fillna('') + " " +
                data['style'].fillna('')
        )

        # TF-IDF 벡터화
        tfidf = TfidfVectorizer(stop_words='english')
        tfidf_matrix = tfidf.fit_transform(data['combined_features'])

        # ID -> Index 매핑
        indices = pd.Series(data.index, index=data['id']).drop_duplicates()

        print(f"[AI] 학습 완료! {len(data)}개 상품 로드됨.")

    except Exception as e:
        print(f"[AI] DB 연결 실패: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 앱 시작 시 실행
    load_data_and_train_model()
    yield
    # 앱 종료 시 실행 (필요하면 추가)

app = FastAPI(lifespan=lifespan)

@app.get("/recommend/{item_id}")
def recommend(item_id: int):
    # 데이터가 없거나 모델이 없으면 에러 방지
    if data is None or indices is None or item_id not in indices:
        return {"recommendations": []}

    # 해당 상품의 인덱스
    idx = indices[item_id]

    # 유사도 계산
    cosine_sim = linear_kernel(tfidf_matrix[idx], tfidf_matrix)
    sim_scores = list(enumerate(cosine_sim[0]))

    # 정렬 (상위 4개)
    sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
    sim_scores = sim_scores[1:4] # 0번은 자기 자신이므로 제외

    # 결과 추출
    item_indices = [i[0] for i in sim_scores]
    recommend_items = data.iloc[item_indices][['id']].to_dict(orient='records')

    return {"recommendations": recommend_items}

def find_available_port(start_port=8000, end_port=8100):
    for port in range(start_port, end_port):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            if s.connect_ex(('localhost', port)) != 0:
                return port
    return None

if __name__ == "__main__":
    # 명령줄 인자로 포트를 받거나, 없으면 자동 탐색
    port = 8000
    if len(sys.argv) > 1:
        port = int(sys.argv[1])
    else:
        # 자동 포트 할당 (기본 8000부터 시도)
        # 하지만 Spring Boot에서 고정된 포트를 바라보고 있다면 이 방식은 Spring Boot 쪽도 동적으로 바꿔야 함.
        # 일단 사용자가 "자동으로 안되냐"고 했으므로, 가장 간단한 방법은
        # 1. Python이 빈 포트를 찾아서 실행하고
        # 2. 그 포트 번호를 파일 등에 기록하거나 출력하면
        # 3. Spring Boot가 그걸 읽어서 호출하는 방식임.
        # 그러나 Spring Boot가 이미 실행 중인 상태에서 Python을 켜는 구조라면(현재 CorsApplication에서 실행 중)
        # Spring Boot가 Python 실행 시 포트를 지정해서 넘겨주는 게 가장 깔끔함.
        pass

    # 여기서는 "Spring Boot가 실행할 때 빈 포트를 찾아서 인자로 넘겨주는 방식"을 채택하기 위해
    # Python 코드는 인자로 받은 포트로 실행되도록 수정함.
    uvicorn.run(app, host="0.0.0.0", port=port)
