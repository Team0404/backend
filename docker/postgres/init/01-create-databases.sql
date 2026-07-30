-- 서비스별 개별 데이터베이스 생성 (Database per Service)
-- postgres 컨테이너 최초 기동 시 1회 실행됨.
-- 각 서비스는 자신의 DB에만 연결한다.

CREATE DATABASE user_service;
CREATE DATABASE hub_service;
CREATE DATABASE company_service;
CREATE DATABASE order_service;
CREATE DATABASE delivery_service;
CREATE DATABASE slack_service;
