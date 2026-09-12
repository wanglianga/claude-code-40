# 社区失能老人辅具租赁与维修回收平台

## 原始需求

建设城市社区失能老人辅具租赁与维修回收平台，可采用 Vue 3、Spring Boot 和 PostgreSQL。社区工作人员为老人建立档案，记录失能等级、居住环境、照护人、医保或补贴资格、需要的轮椅、助行器、防褥疮垫、护理床或洗浴椅。评估师入户后确认门宽、电梯、床边空间、卫生间尺寸和照护人操作能力，再生成辅具建议。家属确认租赁后，平台安排库存、配送、安装、押金、租金和补贴申请。使用期间，家属或照护员提交磨损、异响、尺寸不适、老人摔倒或不会操作等反馈；平台根据风险生成维修、换型或再次评估。若老人住院、去世、搬家、补贴资格变化或辅具损坏，平台要把租赁、维修、回收、押金、消毒和再入库串成同一件辅具的生命周期。回收后，消毒质检和再次上架结果会影响库存周转和补贴核销。平台还要把押金、租金、补贴和维修费用分开核算，家属查询时能看到每笔费用对应的辅具状态和服务动作。平台还要按老人、辅具和补贴生成双维度档案，社区可以看到某件辅具服务过哪些家庭，也能看到某位老人经历过哪些评估、租赁、维修和回收。

## 技术栈与架构

- **前端**：Vue 3 + Vite + Element Plus + Pinia + Vue Router（nginx 非 root 镜像托管，反向代理 `/api`）
- **后端**：Spring Boot 3 (Java 17) + Spring Data JPA + Spring Security(JWT) + Actuator
- **数据库**：PostgreSQL 16（仅 compose 内部网络，不发布宿主端口）

```
宿主 :${CC_PUBLISH_PORT} ──► frontend(nginx:8080) ──/api──► backend(spring:8080) ──► postgres:5432
```

## 一键启动（验证方式 = 宿主 docker compose up）

```bash
cp .env.example .env        # 可选：环境变量已提供 CC_PUBLISH_PORT / COMPOSE_PROJECT_NAME 时可直接用
docker compose up -d --build
docker compose ps           # 等待 backend healthy
docker compose port frontend 8080   # 查看实际映射端口（= $CC_PUBLISH_PORT）
```

浏览器访问 `http://host.docker.internal:${CC_PUBLISH_PORT}`（或宿主机对应地址）。

停止并释放资源：

```bash
docker compose down         # 加 -v 可同时清空数据库卷，下次启动重新灌入演示数据
```

首次启动时后端自动建表并写入演示数据（4 位老人、6 种型号、15 件辅具、6 个租赁单、完整生命周期事件与分账费用）。

## 演示账号（逐角色）

| 角色 | 用户名 | 密码 | 权限说明 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 全部功能；补贴审核 |
| 社区工作人员 | `staff` | `staff123` | 建档、发起评估/租赁、反馈处置、结案、补贴申请、退款 |
| 评估师 | `assessor` | `assess123` | 入户评估（门宽/电梯/床边/卫生间/照护人能力）并生成辅具建议 |
| 家属（张桂兰之子） | `family` | `family123` | 确认租赁、在线缴费、提交使用反馈、查看自家费用与档案 |
| 家属（刘建国之女） | `family2` | `family123` | 同上（另一位老人） |
| 仓储运维 | `warehouse` | `ware123` | 配送/安装、维修执行、回收消毒、质检上架/报废 |

## 核心业务流

1. **建档**：`staff` 在「老人档案」建立档案（失能等级、居住环境、照护人、医保/补贴资格、所需辅具）。
2. **评估**：`staff` 发起评估并指派 → `assessor` 在「入户评估」回填门宽、电梯、床边空间、卫生间尺寸、照护人操作能力，生成辅具建议（类别+型号+说明）。
3. **租赁**：`staff` 按评估建议建租赁单（自动锁定在库辅具，生成押金单）→ `family` 在「租赁订单」确认（押金到账）→ `warehouse` 配送、安装起租（生成首月租金）。
4. **使用反馈**：`family` 提交磨损/异响/尺寸不适/摔倒/不会操作反馈（摔倒自动高风险）→ `staff` 处置为 **维修**（生成维修单）/ **换型**（旧件回收、新件出库）/ **再次评估**（生成评估单）/ 无需上门。
5. **维修**：`warehouse` 开始维修 → 完成维修录入费用（维修费单独记账，租约仍在则辅具恢复「租赁中」）。
6. **结案回收**：老人住院/去世/搬家/补贴资格变化/辅具损坏/到期 → `staff` 结案，辅具变为「已回收待消毒」并生成押金退还单 → `warehouse` 消毒 → 质检：**合格再上架**（累计租期+1，关联补贴自动核销）或 **不合格报废**。
7. **费用分账**：押金、租金、补贴、维修费分类型记账；家属在「费用中心」看到每笔费用对应的辅具、辅具状态与服务动作，可在线支付待缴费用；`staff` 办理押金退还。
8. **双维度档案**：「双维度档案」页按老人查看其全部评估/租赁/维修/回收/费用事件；按辅具查看其服务过的家庭与完整生命周期。

## 目录结构

```
├── docker-compose.yml          # app(frontend) + backend + postgres
├── .env.example
├── backend/                    # Spring Boot 后端
│   ├── Dockerfile              # 多阶段：maven 构建 → jre 运行（非 root + HEALTHCHECK）
│   └── src/main/java/com/community/assist/
│       ├── model/              # 11 个实体 + 全部枚举
│       ├── repo/               # Spring Data JPA 仓库
│       ├── web/                # 11 个 REST 控制器
│       ├── service/            # 当前用户/事件记录/业务异常
│       └── config/             # 安全(JWT)、异常处理、演示数据初始化
└── frontend/                   # Vue 3 前端
    ├── Dockerfile              # 多阶段：node 构建 → nginx 非 root 运行（HEALTHCHECK）
    ├── nginx.conf              # 静态托管 + /api 反代
    └── src/views/              # 登录/工作台/档案/评估/库存/租赁/反馈/维修/费用/补贴/双维度档案
```

## 主要 API

| 模块 | 端点 |
|---|---|
| 认证 | `POST /api/auth/login`、`GET /api/auth/me` |
| 老人 | `GET/POST/PUT /api/elderly`、`GET /api/elderly/{id}/timeline` |
| 评估 | `GET/POST /api/assessments`、`PUT /api/assessments/{id}/complete` |
| 辅具 | `GET /api/devices/models`、`GET/POST /api/devices/units`、`GET /api/devices/units/{id}/lifecycle`、`POST .../disinfect`、`POST .../qc` |
| 租赁 | `GET/POST /api/rentals`、`POST /api/rentals/{id}/confirm|deliver|install|close|cancel` |
| 反馈 | `GET/POST /api/feedback`、`POST /api/feedback/{id}/handle` |
| 维修 | `GET /api/repairs`、`POST /api/repairs/{id}/start|finish` |
| 费用 | `GET /api/payments`、`POST /api/payments/{id}/pay|refund` |
| 补贴 | `GET/POST /api/subsidies`、`POST /api/subsidies/{id}/review|writeoff` |
| 看板 | `GET /api/dashboard/stats` |

## 验证要点

- `docker compose ps` 三个服务全部 Up（backend 为 healthy）。
- 登录页可登录各角色账号；家属 `family` 可确认待确认订单、缴纳租金、提交反馈。
- `staff` 结案后辅具进入回收流；`warehouse` 消毒+质检合格后库存「可租」+1，对应补贴自动核销（见辅具生命周期事件）。
- 「双维度档案」中，辅具 `XY01-001` 可见完整生命周期（租赁→回收→消毒→质检→再上架→补贴核销）。
