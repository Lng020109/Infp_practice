/* =========================================================
   전역 변수
========================================================= */

let currentCategory = "all";

let allPosts = [];

// 게시글 페이지네이션
const POSTS_PER_PAGE = 10;
let currentPage = 1;
let lastFilterState = null;

let currentPost = null;

let localReplies = {};

let socket = null;

let reconnectTimer = null;

let currentUser = getCurrentUser();

let unreadChatCount =
    Number(localStorage.getItem("unreadChatCount")) || 0;

window.addEventListener("DOMContentLoaded", function() {
    const badge = document.getElementById("chat-badge");

    if (unreadChatCount > 0) {
        badge.textContent = unreadChatCount;
        badge.style.display = "inline-flex";
    }
});


/* =========================================================
   테스트용 데이터
========================================================= */

const demoPosts = [
    {
        id: "demo1",
        author: "여행자A",
        username: "여행자A",
        title: "부산에서 꼭 가봐야 할 맛집 3곳",
        content:
            "해운대와 광안리 주변에서 찾은 맛집들을 공유해요. 바다 보면서 먹기 좋은 곳 위주입니다.",
        category: "food",
        likeCount: 18,
        dislikeCount: 0,
        viewCount: 142,
        createdAt: "2026-09-07T10:00:00"
    },
    {
        id: "demo2",
        author: "파도타기",
        username: "파도타기",
        title: "제주도 2박 3일 코스 추천해주세요!",
        content:
            "렌터카로 움직일 예정인데 맛집까지 포함해서 추천받고 싶어요.",
        category: "question",
        likeCount: 12,
        dislikeCount: 0,
        viewCount: 97,
        createdAt: "2026-09-06T13:00:00"
    },
    {
        id: "demo3",
        author: "여행좋아",
        username: "여행좋아",
        title: "강릉 바다 보면서 먹은 카페",
        content:
            "안목해변 근처에서 발견한 분위기 좋은 카페입니다. 일몰 시간대가 특히 예뻐요.",
        category: "recommend",
        likeCount: 24,
        dislikeCount: 1,
        viewCount: 201,
        createdAt: "2026-09-05T17:30:00"
    },
    {
        id: "demo4",
        author: "맛집탐험가",
        username: "맛집탐험가",
        title: "서울 데이트 맛집 추천",
        content:
            "데이트하기 좋은 분위기의 서울 맛집을 찾고 있습니다. 가격대는 1인 3만원 정도 생각하고 있어요.",
        category: "food",
        likeCount: 8,
        dislikeCount: 0,
        viewCount: 75,
        createdAt: "2026-09-04T12:00:00"
    }
];


/* =========================================================
   유틸
========================================================= */

function escapeHtml(value) {

    if (value == null) {return "";}
	return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function formatDate(value) {
    if (!value) {return "";}

    const date = new Date(value);

    if (isNaN(date.getTime())) {
        return "";
    }

    return date.toLocaleString("ko-KR", {
        year: "numeric",
        month: "numeric",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });
}


function formatTime(value) {

    const date = new Date(value);

    if (isNaN(date.getTime())) {
        return "";
    }

    return date.toLocaleTimeString(
        "ko-KR",
        {
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}


function categoryName(category) {

    const categories = {

        food: "🍽️ 맛집",
        travel: "✈️ 여행",
        question: "❓ 질문",
        recommend: "📍 추천"

    };

    return categories[category] || "여행";
}


function getCurrentUser() {
    try {
        const user =
            JSON.parse(
                sessionStorage.getItem(
                    "currentLoggedInUser"
                ) || "null"
            );

        return user;
    } catch (e) {
        return null;
    }
}


function getUsername() {
    const user = getCurrentUser();

    if (!user) {
        return null;
    }

    return (
        user.username ||
        user.name ||
        user.id ||
        null
    );
}


/* =========================================================
   게시글 불러오기
========================================================= */

async function loadPosts() {

    try {

        const response =
            await fetch("/api/community");

        if (!response.ok) {

            throw new Error(
                "게시글 조회 실패"
            );
        }

        const posts =
            await response.json();

        if (
            Array.isArray(posts) &&
            posts.length
        ) {allPosts = posts;}
		else {allPosts = demoPosts;}


        // 게시글별 댓글 불러오기

        for (const post of allPosts) {
            try {
                const replyResponse =
                    await fetch(
                        `/api/community/${post.id}/replies`
                    );
                if (replyResponse.ok) {

                    localReplies[post.id] =
                        await replyResponse.json();
                } else {
                    localReplies[post.id] = [];
                }
            } catch (error) {

                console.error(
                    `게시글 ${post.id} 댓글 조회 실패:`,
                    error
                );
                localReplies[post.id] = [];
            }
        }
    } catch (error) {
        console.log(
            "백엔드 연결 실패. 테스트 데이터를 표시합니다."
        );
        allPosts = demoPosts;
    }
    renderPosts();
    renderPopular();

    // 마이페이지에서 전달한 게시글 ID가 있으면
    // 해당 게시글의 상세 화면을 바로 엽니다.
    const params = new URLSearchParams(location.search);
    const postId = params.get("id");

    if (postId) {
        const targetPost = allPosts.find(
            post => String(post.id) === String(postId)
        );

        if (targetPost) {
            openPost(targetPost.id);
        }
    }
}


/* =========================================================
   게시글 표시
========================================================= */

function renderPosts() {

    const list =
        document.getElementById(
            "post-list"
        );

    const search =
        document
            .getElementById("search-input")
            .value
            .trim()
            .toLowerCase();

    const sort =
        document
            .getElementById("sort-select")
            .value;

    const filterState = `${currentCategory}|${search}|${sort}`;

    if (lastFilterState !== null && lastFilterState !== filterState) {
        currentPage = 1;
    }

    lastFilterState = filterState;


    let posts =
        allPosts.filter(post => {

            const categoryMatch =
                currentCategory === "all" ||
                post.category === currentCategory;

            const searchMatch =
                !search ||

                String(post.title || "")
                    .toLowerCase()
                    .includes(search) ||

                String(post.content || "")
                    .toLowerCase()
                    .includes(search) ||

                String(post.author || "")
                    .toLowerCase()
                    .includes(search);

            return (
                categoryMatch &&
                searchMatch
            );
        });

    if (sort === "popular") {
        posts.sort(
            (a, b) =>
                (b.likeCount || 0) -
                (a.likeCount || 0)
        );
    } else if (sort === "views") {
        posts.sort(
            (a, b) =>
                (b.viewCount || 0) -
                (a.viewCount || 0)
        );
    } else {
        posts.sort(
            (a, b) =>
                new Date(b.createdAt || 0) -
                new Date(a.createdAt || 0)
        );
    }

    if (!posts.length) {
        list.innerHTML = `
            <div class="empty">
                <i class="fa-solid fa-map-location-dot"></i>
                <p>
                    조건에 맞는 게시글이 없습니다.
                </p>
            </div>
        `;

        renderPagination(0);
        return;
    }

    const totalPages =
        Math.ceil(
            posts.length / POSTS_PER_PAGE
        );

    if (currentPage > totalPages) {
        currentPage = totalPages;
    }

    const startIndex =
        (currentPage - 1) *
        POSTS_PER_PAGE;

    const paginatedPosts =
        posts.slice(
            startIndex,
            startIndex + POSTS_PER_PAGE
        );

    list.innerHTML =
        paginatedPosts
            .map(createPostHtml)
            .join("");

    renderPagination(totalPages);
}

/* =========================================================
   페이지네이션
========================================================= */

function renderPagination(totalPages) {

    let pagination =
        document.getElementById("post-pagination");
    if (!pagination) {
        pagination =
            document.createElement("div");
        pagination.id =
            "post-pagination";
        const list =
            document.getElementById("post-list");
        list.parentNode.insertBefore(
            pagination,
            list.nextSibling
        );
    }

    if (totalPages <= 1) {
        pagination.innerHTML = "";
        pagination.style.display = "none";
        return;
    }

    pagination.style.display = "flex";
    pagination.style.justifyContent = "center";
    pagination.style.alignItems = "center";
    pagination.style.gap = "6px";
    pagination.style.margin = "20px 0";

    let html = `
        <button
            type="button"
            onclick="changePostPage(${currentPage - 1})"
            ${currentPage === 1 ? "disabled" : ""}
            style="padding:6px 10px; border:1px solid #ddd; background:#fff; border-radius:6px; cursor:pointer;"
        >
            ‹
        </button>
    `;

    for (
        let page = 1;
        page <= totalPages;
        page++
    ) {

        html += `
            <button
                type="button"
                onclick="changePostPage(${page})"
                style="padding:6px 10px; border:1px solid #ddd; background:${page === currentPage ? "#eee" : "#fff"}; border-radius:6px; cursor:pointer; font-weight:${page === currentPage ? "bold" : "normal"};"
            >
                ${page}
            </button>
        `;
    }

    html += `
        <button
            type="button"
            onclick="changePostPage(${currentPage + 1})"
            ${currentPage === totalPages ? "disabled" : ""}
            style="padding:6px 10px; border:1px solid #ddd; background:#fff; border-radius:6px; cursor:pointer;"
        >
            ›
        </button>
    `;
    pagination.innerHTML = html;
}

function changePostPage(page) {
    if (page < 1) {
        return;
    }

    currentPage = page;

    renderPosts();

    const list =
        document.getElementById(
            "post-list"
        );

    if (list) {

        list.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }
}

/* =========================================================
   게시글 HTML
========================================================= */

function createPostHtml(post) {
    const replyCount =
        (localReplies[post.id] || []).length;
    const userKey =
        post.username ||
        post.author ||
        "";
    return `
        <div
            class="post-card"
            onclick="openPost('${escapeHtml(post.id)}')"
        >
            <div class="post-top">
                <div class="user-icon">
                    ${userKey
            ?
            `
                        <img
                            src="/api/member/${encodeURIComponent(userKey)}/profile-image"
                            onerror="this.style.display='none'; this.nextElementSibling.style.display='block';"
                        >
                    `
            :
            ""
        }
                    <i
                        class="fa-solid fa-user"
                        style="${userKey
            ? "display:none;"
            : ""
        }"
                    ></i>
                </div>

                <span class="post-user-name">
                    ${escapeHtml(post.author || post.username)}
                </span>

                <span class="post-category">
                    ${categoryName(post.category)}
                </span>

                <span class="post-date">
                    ${formatDate(post.createdAt)}
                </span>
            </div>
			
            <h3 class="post-title">
                ${escapeHtml(post.title)}
            </h3>

            <p class="post-content">
                ${escapeHtml(post.content)}
            </p>

            <div class="post-footer">
                <span>
                    <i class="fa-regular fa-thumbs-up"></i>
                    ${post.likeCount || 0}
                </span>

                <span>
                    <i class="fa-regular fa-eye"></i>
                    ${post.viewCount || 0}
                </span>

                <span class="comment">
                    <i class="fa-regular fa-comment"></i>
                    ${replyCount} 댓글
                </span>
            </div>
        </div>
    `;
}

/* =========================================================
   인기 게시글
========================================================= */

function renderPopular() {
    const row =
        document.getElementById(
            "popular-row"
        );

    const popular =
        [...allPosts]
            .sort(
                (a, b) =>
                    (b.likeCount || 0) -
                    (a.likeCount || 0)
            )
            .slice(0, 4);

    row.innerHTML =
        popular
            .map(
                (post, index) => `

                    <div
                        class="popular-card"
                        onclick="openPost('${escapeHtml(post.id)}')"
                    >

                        <span class="popular-rank">

                            TOP ${index + 1}

                        </span>

                        <b>

                            ${escapeHtml(post.title)}

                        </b>

                        <small>
                            👍 ${post.likeCount || 0}

                            ·
                            👁 ${post.viewCount || 0}
                        </small>

                    </div>

                `
            )
            .join("");
}


/* =========================================================
   카테고리
========================================================= */

function selectCategory(
    button,
    category
) {

    currentCategory = category;


    document
        .querySelectorAll(".category-btn")
        .forEach(
            btn =>
                btn.classList.remove("active")
        );


    button.classList.add("active");

    currentPage = 1;

    renderPosts();
}


/* =========================================================
   글쓰기
========================================================= */

function openWriteModal() {

    const user = getCurrentUser();

    if (!user) {
        alert("로그인이 필요한 기능입니다.");
        return;
    }

    document
        .getElementById("write-modal")
        .classList.add("show");
}


function closeWriteModal() {
    document
        .getElementById("write-modal")
        .classList.remove("show");
}


async function createPost() {
    const title =
        document
            .getElementById("post-title")
            .value
            .trim();

    const content =
        document
            .getElementById("post-content")
            .value
            .trim();

    const category =
        document
            .getElementById("post-category")
            .value;

    if (!title) {
        alert("제목을 입력해주세요.");
        return;
    }

    if (!content) {
        alert("내용을 입력해주세요.");
        return;
    }

    const username =
        getUsername();

    if (!username) {
        alert("로그인이 필요한 기능입니다.");
        return;
    }

    try {
        const response =
            await fetch(
                "/api/community",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":"application/json"
                    },
                    body:JSON.stringify({
                            author:username,
                            username:username,
                            title:title,
                            content:content,
                            category:category
                        })
                }
            );

        if (!response.ok) {
            let errorMessage =
                "게시글 등록에 실패했습니다.";
            try {
                const text =
                    await response.text();
					
                if (text) {
                    errorMessage = text;
                }

            } catch (e) {
                console.error("서버 오류 메시지 읽기 실패:", e);
            }

            throw new Error(errorMessage);
        }

        alert("게시글이 등록되었습니다.");

        closeWriteModal();

        loadPosts();

    } catch (error) {
        console.error("게시글 등록 오류:", error);

        alert(error.message || "게시글 등록에 실패했습니다.");
    }


    document
        .getElementById("post-title")
        .value = "";


    document
        .getElementById("post-content")
        .value = "";
}


/* =========================================================
   게시글 상세
========================================================= */

function openPost(id) {
    const post =
        allPosts.find(
            p =>
                String(p.id) ===
                String(id)
        );

    if (!post) {
        return;
    }

    currentPost = post;

    checkPostOwner();

    const likeButton =
        document.getElementById(
            "detail-like-btn"
        );

    likeButton.dataset.liked =
        "false";

    fetch(
        `/api/community/${post.id}/view`,
        {
            method: "POST"
        }
    )
        .then(
            response =>
                response.json()
        )
        .then(
            updatedPost => {
                const index =
                    allPosts.findIndex(
                        p =>
                            String(p.id) ===
                            String(updatedPost.id)
                    );

                if (index !== -1) {
                    allPosts[index] =
                        updatedPost;
                    currentPost =
                        updatedPost;
                    renderPosts();
                    renderPopular();
                }
            }
        )
        .catch(
            error => {
                console.error(
                    "조회수 증가 오류:",
                    error
                );
            }
        );

    document
        .getElementById("detail-title")
        .textContent =
        post.title;

    document
        .getElementById("detail-author-name")
        .textContent =
        post.author ||
        post.username ||
        "익명";

    document
        .getElementById("detail-meta")
        .textContent =
        `${formatDate(post.createdAt)} · ${categoryName(post.category)}`;

    const userKey =
        post.username ||
        post.author ||
        "";

    const profileImg =
        document.getElementById(
            "detail-profile-img"
        );

    const defaultIcon =
        document.getElementById(
            "detail-default-icon"
        );

    if (userKey) {
        profileImg.src =
            `/api/member/${encodeURIComponent(userKey)}/profile-image`;
        profileImg.style.display = "block";
        defaultIcon.style.display = "none";
    } else {
        profileImg.style.display = "none";
        defaultIcon.style.display = "block";
    }


    document
        .getElementById("detail-content")
        .textContent =
        post.content;

    document
        .getElementById("detail-like-count")
        .textContent =
        post.likeCount || 0;

    renderReplies();

    document
        .getElementById("detail-modal")
        .classList.add("show");
}

function closeDetailModal() {

    document
        .getElementById("detail-modal")
        .classList.remove("show");

    currentPost = null;

    // 주소에서 게시글 id를 제거해서
    // 새로고침해도 상세창이 다시 뜨지 않게 합니다.

    const url =
        new URL(
            window.location.href
        );

    url.searchParams.delete("id");

    window.history.replaceState(
        {},
        "",
        url.pathname +
        url.search +
        url.hash
    );
}

/* =========================================================
   좋아요
========================================================= */

async function likeCurrentPost() {
    if (!currentPost) {
        return;
    }

    const username =
        getUsername();

    try {
        const response =
            await fetch(
                `/api/community/${currentPost.id}/like?username=${encodeURIComponent(username)}`,
                {method: "POST"}
            );

        if (!response.ok) {
            throw new Error("좋아요 처리 실패");
        }

        const updatedPost =
            await response.json();

        currentPost =
            updatedPost;

        const index =
            allPosts.findIndex(
                p =>
                    String(p.id) ===
                    String(updatedPost.id)
            );


        if (index !== -1) {
            allPosts[index] =
                updatedPost;
        }

        document
            .getElementById(
                "detail-like-count"
            )
            .textContent =
            updatedPost.likeCount;

        renderPosts();

        renderPopular();

    } catch (error) {

        console.error(
            "좋아요 오류:",
            error
        );

        alert(
            "좋아요 처리에 실패했습니다."
        );
    }
}

/* =========================================================
   댓글
========================================================= */

async function renderReplies() {
    if (!currentPost) {return;}

    const list =
        document.getElementById("reply-list");

    try {
        const response =
            await fetch(
                `/api/community/${currentPost.id}/replies`
            );

        if (!response.ok) {
            throw new Error("답글 조회 실패");
        }

        const replies =
            await response.json();

        if (!replies.length) {
            list.innerHTML = `
                <p style="
                    font-size:11px;
                    color:#9aabb4;
                    margin:12px 0;
                ">
                    첫 번째 댓글을 남겨보세요.
                </p>
            `;
            return;
        }

        list.innerHTML =
            replies
                .map(
                    reply => {
                        // username이 있으면 우선 사용하고,
                        // 없으면 author 사용
                        const replyUserKey =
                            reply.username ||
                            reply.author ||
                            "";

                        return `
                            <div class="reply-item">
                                <div class="reply-user">
                                    <div
                                        class="reply-user-icon"
                                        style="
                                            width: 28px;
                                            height: 28px;
                                            border-radius: 50%;
                                            overflow: hidden;
                                            display: inline-flex;
                                            align-items: center;
                                            justify-content: center;
                                        "
                                    >
                                        ${replyUserKey
                                ?
                                `
                                            <img
                                                src="/api/member/${encodeURIComponent(replyUserKey)}/profile-image"
                                                style="
                                                    width: 100%;
                                                    height: 100%;
                                                    object-fit: cover;
                                                    display: block;
                                                "
                                                onerror="this.style.display='none'; this.nextElementSibling.style.display='block';"
                                            >
                                        `
                                :
                                ""
                            }
                                        <i
                                            class="fa-solid fa-user"
                                            style="${replyUserKey
                                ? "display:none;"
                                : "display:block;"
                            }"
                                        ></i>
                                    </div>
                                    <b>
                                        ${escapeHtml(
                                reply.author ||
                                reply.username
                            )}
                                    </b>
                                    <span class="reply-date">
                                        ${reply.createdAt
                                ? formatDate(
                                    reply.createdAt
                                )
                                : "방금 전"
                            }
                                    </span>
                                </div>

                                <div class="reply-text">
                                    ${escapeHtml(
                                reply.content
                            )}
                                </div>

                                <div class="reply-buttons">
                                    <button>좋아요 </button>

                                    <button
                                        onclick="replyTo('${escapeHtml(reply.author || reply.username)}')"
                                    >
                                        답글
                                    </button>

                                    ${getUsername() &&
                                      reply.username &&
                                      String(reply.username) === String(getUsername())
                                        ? `
                                            <button
                                                type="button"
                                                onclick="editReply(${reply.id})"
                                            >
                                                수정
                                            </button>

                                            <button
                                                type="button"
                                                onclick="deleteReply(${reply.id})"
                                            >
                                                삭제
                                            </button>
                                          `
                                        : ""
                                    }

                                </div>
                            </div>
                        `;
                    }
                )
                .join("");

    } catch (error) {
        console.error("답글 조회 오류:", error);

        list.innerHTML = `
            <p style="
                font-size:11px;
                color:#9aabb4;
                margin:12px 0;
            ">
                답글을 불러오지 못했습니다.
            </p>
        `;
    }
}

async function addReply() {
    if (!currentPost) {
        return;
    }

    const input =
        document.getElementById(
            "reply-input"
        );

    const content =
        input.value.trim();

    if (!content) {
        return;
    }

    const author =
        getUsername();

    if (!author) {
        alert("로그인이 필요한 기능입니다.");
        return;
    }

    try {
        const response =
            await fetch(
                `/api/community/${currentPost.id}/replies`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body:
                        JSON.stringify({
                            author:author,
                            username:author,
                            content:content
                        })
                }
            );


        if (!response.ok) {
            throw new Error("답글 등록 실패");
        }

        const savedReply =
            await response.json();

        if (!localReplies[currentPost.id]) {
            localReplies[currentPost.id] = [];
        }

        localReplies[currentPost.id]
            .push(savedReply);

        input.value = "";

        await renderReplies();

        renderPosts();

    } catch (error) {
        console.error("답글 등록 오류:", error);

        alert("답글 등록에 실패했습니다.");
    }
}


function replyTo(username) {

    const input =
        document.getElementById(
            "reply-input"
        );

    input.value =
        "@" +
        username +
        " ";

    input.focus();
}


/* =========================================================
   댓글 수정
========================================================= */

async function editReply(replyId) {

    if (!currentPost) {
        return;
    }

    const username =
        getUsername();

    if (!username) {
        alert("로그인이 필요한 기능입니다.");
        return;
    }

    const replies =
        localReplies[currentPost.id] || [];

    const reply =
        replies.find(
            item =>
                String(item.id) ===
                String(replyId)
        );

    if (!reply) {
        alert("댓글을 찾을 수 없습니다.");
        return;
    }

    // 화면에서도 한 번 더 본인 댓글인지 확인
    if (
        !reply.username ||
        String(reply.username) !== String(username)
    ) {
        alert("본인이 작성한 댓글만 수정할 수 있습니다.");
        return;
    }

    const content =
        prompt(
            "수정할 댓글 내용을 입력하세요.",
            reply.content || ""
        );

    if (content === null) {
        return;
    }

    if (!content.trim()) {
        alert("댓글 내용을 입력해주세요.");
        return;
    }

    try {

        const response =
            await fetch(
                `/api/community/replies/${replyId}?username=${encodeURIComponent(username)}`,
                {
                    method: "PUT",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify({
                            content:
                                content.trim()
                        })
                }
            );

        if (!response.ok) {

            const errorText =
                await response.text();

            throw new Error(
                errorText ||
                "댓글 수정에 실패했습니다."
            );
        }

        const updatedReply =
            await response.json();

        const index =
            replies.findIndex(
                item =>
                    String(item.id) ===
                    String(replyId)
            );

        if (index !== -1) {
            replies[index] =
                updatedReply;
        }

        alert("댓글이 수정되었습니다.");

        await renderReplies();

        renderPosts();

    } catch (error) {

        console.error(
            "댓글 수정 오류:",
            error
        );

        alert(
            error.message ||
            "댓글 수정에 실패했습니다."
        );
    }
}


/* =========================================================
   댓글 삭제
========================================================= */

async function deleteReply(replyId) {

    if (!currentPost) {
        return;
    }

    const username =
        getUsername();

    if (!username) {
        alert("로그인이 필요한 기능입니다.");
        return;
    }

    const replies =
        localReplies[currentPost.id] || [];

    const reply =
        replies.find(
            item =>
                String(item.id) ===
                String(replyId)
        );

    if (!reply) {
        alert("댓글을 찾을 수 없습니다.");
        return;
    }

    // 화면에서도 한 번 더 본인 댓글인지 확인
    if (
        !reply.username ||
        String(reply.username) !== String(username)
    ) {
        alert("본인이 작성한 댓글만 삭제할 수 있습니다.");
        return;
    }

    const ok =
        confirm(
            "이 댓글을 삭제하시겠습니까?"
        );

    if (!ok) {
        return;
    }

    try {

        const response =
            await fetch(
                `/api/community/replies/${replyId}?username=${encodeURIComponent(username)}`,
                {
                    method: "DELETE"
                }
            );

        if (!response.ok) {

            const errorText =
                await response.text();

            throw new Error(
                errorText ||
                "댓글 삭제에 실패했습니다."
            );
        }

        localReplies[currentPost.id] =
            replies.filter(
                item =>
                    String(item.id) !==
                    String(replyId)
            );

        alert("댓글이 삭제되었습니다.");

        await renderReplies();

        renderPosts();

    } catch (error) {

        console.error(
            "댓글 삭제 오류:",
            error
        );

        alert(
            error.message ||
            "댓글 삭제에 실패했습니다."
        );
    }
}

function handleReplyKey(event) {
    if (event.key === "Enter") {
        addReply();
    }
}

/* =========================================================
   링크 복사
========================================================= */

function copyCurrentPost() {
    if (!currentPost) {
        return;
    }

    const url =
        location.origin +
        location.pathname +
        "?post=" +
        currentPost.id;

    navigator.clipboard
        .writeText(url)
        .then(
            () =>
                alert("게시글 링크를 복사했습니다.")
        )
        .catch(
            () =>
                alert("링크 복사에 실패했습니다.")
        );
}

/* =========================================================
   채팅창 열기 / 닫기
========================================================= */

function toggleChat() {

    const panel =
        document.getElementById(
            "chat-panel"
        );

    panel.classList.toggle("show");

    if (
        panel.classList.contains("show")
    ) {
        // 채팅창을 열면 읽지 않은 메시지 초기화
        unreadChatCount = 0;

        localStorage.setItem(
            "unreadChatCount",
            "0"
        );


        const badge =
            document.getElementById("chat-badge");

        badge.textContent = "";
        badge.style.display = "none";

        // 최신 메시지가 보이도록 맨 아래로 이동

        const box =
            document.getElementById("chat-messages");

        setTimeout(() => {
			box.scrollTop = box.scrollHeight;
        }, 0);

        document
            .getElementById("chat-input")
            .focus();
    }
}

function showUnreadChatBadge() {
    unreadChatCount++;

    localStorage.setItem(
        "unreadChatCount",
        unreadChatCount
    );


    const badge = document.getElementById("chat-badge");

    badge.textContent = unreadChatCount;

    badge.style.display = "block";
}

/* =========================================================
   이전 채팅 불러오기
========================================================= */

async function loadChatHistory() {

    try {

        const response =
            await fetch("/api/community/chat/history");

        if (!response.ok) {
            throw new Error("채팅 기록 조회 실패");
        }


        const messages =
            await response.json();

        const box =
            document.getElementById(
                "chat-messages"
            );

        // 기존 채팅 비우기

        box.innerHTML = "";

        // 이전 채팅 표시

        messages.forEach(
            data => {

                addChatMessage(

                    data.nickname ||
                    data.username ||
                    "여행자",

                    data.message,

                    data.createdAt
                        ? formatTime(data.createdAt)
                        : "",

                    data.username ===
                    getUsername()
                );
            }
        );

        setTimeout(() => {
            box.scrollTop =
                box.scrollHeight;
        }, 100);

        updateChatStatus("실시간 채팅 연결 중...");

    } catch (error) {
        console.error("이전 채팅 불러오기 실패:", error);

        updateChatStatus("채팅 서버 연결 중...");
    }
}

/* =========================================================
   실시간 채팅 WebSocket
========================================================= */

function connectChat() {
    if (
        location.protocol === "file:"
    ) {
        updateChatStatus("브라우저 테스트 모드");
        return;
    }

    const protocol =
        location.protocol === "https:"
            ? "wss:"
            : "ws:";

    const wsUrl =
        protocol +
        "//" +
        location.host +
        "/ws/chat";


    try {
        socket =
            new WebSocket(
                wsUrl
            );
        socket.onopen =
            function() {
                updateChatStatus("실시간 채팅 연결됨");

                // 서버에 로그인 사용자 전달
                socket.send(
                    JSON.stringify({
                        type:
                            "join", username:getUsername()
                    })
                );
            };

        socket.onmessage =
            function(event) {
                try {
                    const data =
                        JSON.parse(event.data);

                    if (data.type === "error"
                    ) {
                        alert(data.message || "부적절한 내용이 포함되어 있습니다.");
                        return;
                    }

                    if (
                        data.type === "message"
                    ) {

                        addChatMessage(
                            data.nickname ||
                            data.username || "여행자",
                            data.message,
                            data.time,
                            data.username === getUsername()
                        );

                        // 채팅창이 닫혀 있을 때
                        // 읽지 않은 메시지 증가

                        if (
                            !document
                                .getElementById(
                                    "chat-panel"
                                )
                                .classList.contains("show")
                        ) {
                            showUnreadChatBadge();
                        }
                    }
                } catch (error) {
                    console.log(
						"채팅 데이터 오류",
						error);
                }
            };

        socket.onclose =
            function() {
                updateChatStatus(
					"채팅 서버 재연결 중..."
				);

                clearTimeout(reconnectTimer);

                reconnectTimer =
                    setTimeout(
                        connectChat,
                        3000
                    );
            };


        socket.onerror =
            function() {

                updateChatStatus(
                    "채팅 서버 연결 실패"
                );
            };

    } catch (error) {

        updateChatStatus(
            "채팅 서버 연결 실패"
        );
    }
}


/* =========================================================
   채팅 상태 표시
========================================================= */

function updateChatStatus(text) {

    document
        .getElementById(
            "chat-status-text"
        )
        .textContent =
        text;
}

/* =========================================================
   채팅 메시지 추가
========================================================= */

function addChatMessage(
    username,
    message,
    time,
    isMe
) {

    const box =
        document.getElementById(
            "chat-messages"
        );

    const div =
        document.createElement(
            "div"
        );

    div.className =
        "message" +
        (isMe ? " me" : "");

    div.innerHTML = `
        <div class="message-name">
            ${escapeHtml(username)}
            <span class="message-time">
                ${escapeHtml(
        time ||
        formatTime(
            new Date()
        )
    )}
            </span>
        </div>

        <span class="bubble">
            ${escapeHtml(message)}
        </span>
    `;

    box.appendChild(div);

    setTimeout(() => {
        box.scrollTop =
            box.scrollHeight;
    }, 0);
}

/* =========================================================
   채팅 전송
========================================================= */

function sendChat() {
    const input =
        document.getElementById(
            "chat-input"
        );

    const message =
        input.value.trim();

    if (!message) {
        return;
    }

    /*
        WebSocket 연결 상태면
        서버로 전송

        서버에서 DB 저장 후
        모든 접속자에게 다시 전송됨
    */

    if (
        socket &&
        socket.readyState ===
        WebSocket.OPEN
    ) {

        socket.send(

            JSON.stringify({

                type:
                    "message",

                username:
                    getUsername(),

                message:
                    message,

                time:
                    formatTime(
                        new Date()
                    )

            })
        );

    } else {

        /*
            서버 연결이 안 된 경우
            화면에만 표시
        */
        addChatMessage(
            getUsername(),
            message,
            "방금 전",
            true
        );
    }

    input.value = "";
}

function handleChatKey(event) {
    if (
        event.key === "Enter"
    ) {
        sendChat();
    }
}


/* =========================================================
   뒤로가기
========================================================= */

function goBack() {
    location.href = "/map";
}

/* =========================================================
   모달 바깥 클릭
========================================================= */

window.addEventListener(
    "click",
    function(event) {

        if (
            event.target.classList.contains(
                "modal"
            )
        ) {

            event.target.classList.remove(
                "show"
            );
        }
    }
);


/* =========================================================
   시작
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    async function() {

        // 게시글 불러오기
        loadPosts();

        // 이전 채팅 먼저 불러오기
        await loadChatHistory();

        // 이후 실시간 채팅 연결
        connectChat();
    }
);

function checkPostOwner() {
    const user =
        getCurrentUser();
    const editBtn =
        document.getElementById(
            "detail-edit-btn"
        );
    const deleteBtn =
        document.getElementById(
            "detail-delete-btn"
        );

    if (!currentPost || !user) {
        editBtn.style.display = "none";
        deleteBtn.style.display = "none";
        return;
    }

    const username = getUsername();

    // 게시글 작성자의 username과
    // 현재 로그인한 username 비교

    if (
        (
            currentPost.username ||
            currentPost.author
        ) === username
    ) {

        editBtn.style.display =
            "inline-block";

        deleteBtn.style.display =
            "inline-block";
    } else {
        editBtn.style.display = "none";
        deleteBtn.style.display = "none";
    }
}

async function deleteCurrentPost() {
    if (!currentPost) {
        return;
    }

    const ok = confirm("이 게시글을 삭제하시겠습니까?\n답글도 모두 삭제됩니다.");

    if (!ok) {
        return;
    }

    try {
        const response = await fetch(
                `/api/community/${currentPost.id}`,
                {
                    method: "DELETE"
                }
            );


        if (!response.ok) {
            throw new Error("게시글 삭제 실패");
        }


        alert("게시글이 삭제되었습니다.");

        // 상세 화면 닫기
        closeDetailModal();

        // 게시글 목록 다시 불러오기
        loadPosts();
    } catch (error) {
        console.error(error);
        alert("게시글 삭제 중 오류가 발생했습니다.");
    }
}

async function editCurrentPost() {
    if (!currentPost) {
        return;
    }


    const title =prompt("게시글 제목을 입력하세요.", currentPost.title);

    if (title === null) {
        return;
    }

    const content =prompt("게시글 내용을 입력하세요.", currentPost.content);

    if (content === null) {
        return;
    }


    if (title.trim() === "" || content.trim() === "") {
        alert("제목과 내용을 입력해주세요.");
        return;
    }

    try {

        const response =
            await fetch(
                `/api/community/${currentPost.id}`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body:
                        JSON.stringify({
                            title: title,
                            content: content,
                            category: currentPost.category
                        })
                }
            );

        if (!response.ok) {
            throw new Error("게시글 수정 실패");
        }


        const updatedPost = await response.json();

        currentPost = updatedPost;

        alert("게시글이 수정되었습니다.");

        // 목록 새로고침
        loadPosts();
    } catch (error) {
        console.error(error);
        alert("게시글 수정 중 오류가 발생했습니다.");
    }
}