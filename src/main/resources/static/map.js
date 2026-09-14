const KAKAO_REST_API_KEY = "e52666e6bae05c61e7906f72e252a161";
const OPENWEATHER_API_KEY = "4add407be1d02427baf6b7d9a111284a";
let currentWeatherRecommendFood = "전집";
let currentSearchRegion = "대전";

// [추가] 지역별 추천 메뉴 유지용 변수
let lastRecommendedRegion = "";
let currentRecommendedFood = null;

// SVG 커스텀 핀 (파란색 / 노란색)
const BLUE_PIN = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="42" viewBox="0 0 32 42">
        <path fill="#2b82d9" stroke="#ffffff" stroke-width="2" d="M16 0C7.16 0 0 7.16 0 16c0 10.8 16 26 16 26s16-15.2 16-26C32 7.16 24.84 0 16 0z"/>
        <circle cx="16" cy="15" r="6" fill="#ffffff"/>
    </svg>
`);

const YELLOW_PIN = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="28" height="38" viewBox="0 0 32 42">
        <path fill="#f1b40e" stroke="#ffffff" stroke-width="2" d="M16 0C7.16 0 0 7.16 0 16c0 10.8 16 26 16 26s16-15.2 16-26C32 7.16 24.84 0 16 0z"/>
        <circle cx="16" cy="15" r="5" fill="#ffffff"/>
    </svg>
`);

let mainMap = null;
let modalMap = null;
let mainMarkers = [];
let modalMarkers = [];
let mainInfoWindow = null;
let modalInfoWindow = null;
let currentSelectedPlace = null;       // [맛집 전용] 기존 로직 유지
let currentSelectedTourSpot = null;    // [관광지 전용] 신규 분리
let currentReviewType = 'RESTAURANT';  // 'RESTAURANT' 또는 'TOUR' 구분 플래그
let currentPlaces = [];
let currentTourPlaces = [];            // <-- [추가] 관광지 목록 보관용

// 찜 목록 저장 배열
let favoriteList = [];

// 로그인된 사용자 정보 보관
let currentLoggedInUser = null;

document.addEventListener("DOMContentLoaded", function() {
    // 저장된 로그인 정보 불러오기
    const savedUser = sessionStorage.getItem("currentLoggedInUser");
    if (savedUser) {
        try {
            currentLoggedInUser = JSON.parse(savedUser);
            document.getElementById("header-auth-btn").innerText = "로그아웃";
            document.getElementById("my-page-btn").style.display = "flex";
        } catch (e) {
            sessionStorage.removeItem("currentLoggedInUser");
            currentLoggedInUser = null;
        }
    }

    initMainMap();
    loadPopularKeywords();

    if (currentLoggedInUser) {
        loadFavoriteList();
    }

    fetchRestaurants("대전 맛집");
});

// 1. 메인 지도 초기화
function initMainMap() {
    const container = document.getElementById('map');
    const options = {
        center: new kakao.maps.LatLng(36.3504, 127.3845),
        level: 4
    };
    mainMap = new kakao.maps.Map(container, options);
    mainInfoWindow = new kakao.maps.InfoWindow({ zIndex: 10 });
}

// 검색 실행 및 인기검색어 카운트 반영
function searchByInput() {
    const query = document.getElementById('search-input').value.trim();
    if (!query) return alert("검색어를 입력해주세요.");

    fetch(`/api/search/count?keyword=${encodeURIComponent(query)}`, { method: 'POST' })
        .then(response => {
            if (!response.ok) throw new Error(`HTTP 상태코드: ${response.status}`);
            loadPopularKeywords();
        })
        .catch(err => console.error("카운트 증가 실패 상세:", err));

    fetchRestaurants(query);
}

function searchTag(tag) {
    document.getElementById('search-input').value = tag;
    searchByInput();
}

// 현재 위치 기준 주변 맛집 찾기
function findNearbyRestaurants() {
    if (!navigator.geolocation) {
        alert("이 브라우저에서는 현재 위치를 확인할 수 없습니다.");
        return;
    }

    const emptyState = document.getElementById('empty-state');
    const listEl = document.getElementById('restaurant-list');
    emptyState.style.display = "none";
    listEl.style.display = "flex";
    listEl.innerHTML = "<p style='text-align:center; color:#888; padding:30px;'>현재 위치 주변 맛집을 찾는 중입니다...</p>";

    navigator.geolocation.getCurrentPosition(
        function(position) {
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;

            const url = `https://dapi.kakao.com/v2/local/search/category.json?category_group_code=FD6&x=${lng}&y=${lat}&radius=3000&sort=distance&size=15`;

            fetch(url, {
                headers: { Authorization: `KakaoAK ${KAKAO_REST_API_KEY}` }
            })
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP 상태코드: ${res.status}`);
                    return res.json();
                })
                .then(data => {
                    currentPlaces = data.documents || [];
                    document.getElementById('search-input').value = "내 주변 맛집";

                    if (currentPlaces.length === 0) {
                        listEl.innerHTML = "<p style='text-align:center; color:#888; padding:30px;'>반경 3km 이내에 맛집이 없습니다.</p>";
                        return;
                    }

                    renderList(currentPlaces);
                })
                .catch(err => {
                    console.error("주변 맛집 조회 오류:", err);
                    listEl.innerHTML = "<p style='text-align:center; color:red; padding:30px;'>주변 맛집을 불러오지 못했습니다.</p>";
                });
        },
        function(error) {
            console.error("현재 위치 조회 오류:", error);
            listEl.innerHTML = "<p style='text-align:center; color:#888; padding:30px;'>현재 위치를 확인할 수 없습니다.</p>";
            alert("주변 맛집을 찾으려면 위치 정보 사용을 허용해주세요.");
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
}

function loadPopularKeywords() {
    fetch('/api/search/popular')
        .then(response => {
            if (!response.ok) throw new Error(`HTTP 상태코드: ${response.status}`);
            return response.json();
        })
        .then(data => {
            const container = document.getElementById('popular-keywords');
            if (!container) return;
            container.innerHTML = '';

            if (!data || data.length === 0) {
                container.innerHTML = '<span style="font-size: 12px; color: #888;">검색 기록이 없습니다.</span>';
                return;
            }

            data.forEach(item => {
                const btn = document.createElement('button');
                btn.className = 'tag-badge';
                btn.innerText = item.keyword;
                btn.onclick = function() {
                    searchTag(item.keyword);
                };
                container.appendChild(btn);
            });
        })
        .catch(error => console.error('인기 검색어 로드 실패:', error));
}

// 주소에서 정확한 시/군/구(또는 광역시) 이름 추출
function extractAccurateRegion(address) {
    if (!address) return "현재";
    const parts = address.split(" ");
    if (parts.length < 1) return "현재";

    const first = parts[0];

    if (first.includes("서울")) return "서울";
    if (first.includes("세종")) return "세종";
    if (first.includes("인천")) return "인천";
    if (first.includes("대전")) return "대전";
    if (first.includes("대구")) return "대구";
    if (first.includes("울산")) return "울산";
    if (first.includes("부산")) return "부산";
    if (first.includes("광주") && !address.includes("경기")) return "광주광역시";
    if (first.includes("제주")) {
        return parts[1] ? parts[1] : "제주";
    }

    if (parts.length >= 2) {
        const second = parts[1];
        if (first.includes("경기") && second.includes("광주")) {
            return "경기 광주";
        }
        return second;
    }

    return first;
}

// 2. 맛집 검색 (FD6 음식점)
async function fetchRestaurants(query) {
    const emptyState = document.getElementById('empty-state');
    const listEl = document.getElementById('restaurant-list');

    emptyState.style.display = "none";
    listEl.style.display = "flex";
    listEl.innerHTML = "<p style='text-align:center; color:#888; padding:30px;'>실시간 맛집을 탐색 중입니다...</p>";

    const url = `https://dapi.kakao.com/v2/local/search/keyword.json?query=${encodeURIComponent(query)}&category_group_code=FD6&size=10`;

    try {
        const res = await fetch(url, {
            headers: { Authorization: `KakaoAK ${KAKAO_REST_API_KEY}` }
        });
        const data = await res.json();
        currentPlaces = data.documents || [];

        const filterBar = document.getElementById('rest-filter-bar');
        const totalCountEl = document.getElementById('rest-total-count');
        if (filterBar) filterBar.style.display = "flex";
        if (totalCountEl) totalCountEl.innerText = `총 ${currentPlaces.length}개 맛집`;

        // [추가] 각 맛집의 좋아요, 리뷰 수, 찜 여부/수를 서버에서 병렬로 수집
        const statsPromises = currentPlaces.map(async place => {
            place.likeCount = 0;
            place.reviewCount = 0;
            place.favCount = 0;

            try {
                // 1. 좋아요 수 조회
                const reactionRes = await fetch(`/api/reactions/${encodeURIComponent(place.id)}`);
                if (reactionRes.ok) {
                    const reactionData = await reactionRes.json();
                    place.likeCount = reactionData.likeCount || 0;
                }
            } catch (e) {}

            try {
                // 2. 리뷰 개수 조회
                const reviewRes = await fetch(`/api/reviews/place/${encodeURIComponent(place.id)}`);
                if (reviewRes.ok) {
                    const reviewData = await reviewRes.json();
                    place.reviewCount = Array.isArray(reviewData) ? reviewData.length : 0;
                }
            } catch (e) {}

            try {
                // 3. MySQL DB favorite 테이블에서 실제 찜 누적 개수 가져오기
                const favRes = await fetch(`/api/favorites/count/${encodeURIComponent(place.id)}`);
                if (favRes.ok) {
                    const count = await favRes.json();
                    place.favCount = Number(count) || 0;
                } else {
                    place.favCount = 0;
                }
            } catch (e) {
                place.favCount = 0;
            }
        });

        await Promise.all(statsPromises);

        // 정렬 및 렌더링 호출
        sortAndRenderRestaurants();

        if (currentPlaces.length > 0) {
            const addr = currentPlaces[0].address_name || currentPlaces[0].road_address_name || "";
            currentSearchRegion = extractAccurateRegion(addr);
            fetchWeatherForHero(currentPlaces[0].y, currentPlaces[0].x, currentSearchRegion);
        }
    } catch (err) {
        console.error(err);
        listEl.innerHTML = "<p style='text-align:center; color:red; padding:30px;'>맛집 정보를 불러오지 못했습니다.</p>";
    }
}

// =========================================================
// 맛집 다중 정렬 처리 (기본순 / 좋아요순 / 리뷰순 / 찜순)
// =========================================================
async function sortAndRenderRestaurants() {
    const sortSelect = document.getElementById("rest-sort-select");
    const sortType = sortSelect ? sortSelect.value : "default";

    // 1. '❤️ 찜 많은 순' 선택 시: MySQL DB 전체에서 찜된 모든 식당을 직접 불러와서 표시
    if (sortType === "favorites") {
        try {
            const res = await fetch('/api/favorites/ranking');
            if (res.ok) {
                const dbList = await res.json();

                // DB 데이터를 화면 표시 규격으로 변환
                currentPlaces = dbList.map(item => ({
                    id: item.placeId,
                    place_name: item.placeName,
                    road_address_name: item.address,
                    address_name: item.address,
                    phone: item.phone,
                    x: item.x,
                    y: item.y,
                    place_url: item.placeUrl,
                    category_name: item.categoryName || '음식점',
                    likeCount: 0,
                    reviewCount: 0,
                    favCount: 0
                }));

                // 각 식당의 좋아요수/리뷰수/찜개수 조회
                const statsPromises = currentPlaces.map(async place => {
                    try {
                        const favRes = await fetch(`/api/favorites/count/${encodeURIComponent(place.id)}`);
                        if (favRes.ok) place.favCount = Number(await favRes.json()) || 0;
                    } catch (e) {}
                    try {
                        const revRes = await fetch(`/api/reviews/place/${encodeURIComponent(place.id)}`);
                        if (revRes.ok) {
                            const data = await revRes.json();
                            place.reviewCount = Array.isArray(data) ? data.length : 0;
                        }
                    } catch (e) {}
                    try {
                        const reacRes = await fetch(`/api/reactions/${encodeURIComponent(place.id)}`);
                        if (reacRes.ok) {
                            const data = await reacRes.json();
                            place.likeCount = data.likeCount || 0;
                        }
                    } catch (e) {}
                });
                await Promise.all(statsPromises);

                // DB 찜 많은 순으로 정렬
                currentPlaces.sort((a, b) => (b.favCount || 0) - (a.favCount || 0));

                const totalCountEl = document.getElementById('rest-total-count');
                if (totalCountEl) totalCountEl.innerText = `총 ${currentPlaces.length}개 맛집`;

                renderList(currentPlaces);
                return;
            }
        } catch (err) {
            console.error("DB 랭킹 로드 실패:", err);
        }
    }

    // 2. 기본순 / 좋아요순 / 리뷰순 선택 시 기존 리스트 정렬
    if (!currentPlaces || currentPlaces.length === 0) return;

    let sorted = [...currentPlaces];

    if (sortType === "likes") {
        sorted.sort((a, b) => (Number(b.likeCount) || 0) - (Number(a.likeCount) || 0));
    } else if (sortType === "reviews") {
        sorted.sort((a, b) => (Number(b.reviewCount) || 0) - (Number(a.reviewCount) || 0));
    }

    renderList(sorted);
}

function renderList(places) {
    const listEl = document.getElementById('restaurant-list');
    listEl.innerHTML = "";
    clearMarkers(mainMarkers);

    if (!places || places.length === 0) {
        listEl.innerHTML = "<p style='text-align:center; color:#888; padding:30px;'>검색 결과가 없습니다.</p>";
        return;
    }

    places.forEach((place, index) => {
        const category = place.category_name.split('>').pop().trim() || '음식점';
        const isFavorite = favoriteList.some(item => item.id === place.id);
        const heartIcon = isFavorite ? "❤️" : "🤍";

        const reviewBadge = place.reviewCount > 0
            ? `<span style="font-size: 11px; color: #2ba6cf; background: #e0f2fe; padding: 2px 6px; border-radius: 4px; margin-left: 6px;">리뷰 ${place.reviewCount}</span>`
            : '';

        const card = document.createElement('div');
        card.className = 'rest-card';
        card.id = `rest-${place.id}`;
        card.innerHTML = `
		            <div class="rest-card-info" onclick='selectPlace(${JSON.stringify(place)})'>
		                <div>
		                    <span class="rest-card-badge">${category}</span>
		                    ${reviewBadge}
		                </div>
		                <h4>${index + 1}. ${place.place_name}</h4>
		                <p>📍 ${place.road_address_name || place.address_name}</p>
		                <p>📞 ${place.phone || '전화번호 정보 없음'}</p>
		            </div>

		            <div style="display: flex; align-items: center;">
		                <div class="reaction-wrap">
		                    <button class="reaction-btn like-btn" id="like-btn-${place.id}" onclick='toggleReaction(event, "${place.id}", "LIKE")'>
		                        👍 <span id="like-count-${place.id}" class="reaction-count">${place.likeCount || 0}</span>
		                    </button>
		                    <button class="reaction-btn dislike-btn" id="dislike-btn-${place.id}" onclick='toggleReaction(event, "${place.id}", "DISLIKE")'>
		                        👎 <span id="dislike-count-${place.id}" class="reaction-count">0</span>
		                    </button>
		                </div>
		                <button class="heart-btn" id="fav-btn-${place.id}" title="찜하기" onclick='toggleFavorite(event, ${JSON.stringify(place)})'>
		                    ${heartIcon}
		                </button>
		            </div>
		        `;

        listEl.appendChild(card);

        loadReactions(place.id);

        if (index === 0) selectPlace(place);
    });
}

// 3. 맛집 선택 시 지도 갱신 & 날씨 조회 & 버튼 활성화
function selectPlace(place) {
    currentSelectedPlace = place;

    localStorage.setItem("restaurantName", place.place_name);
    localStorage.setItem("restaurantAddress", place.road_address_name || place.address_name);
    localStorage.setItem("restaurantLat", place.y);
    localStorage.setItem("restaurantLng", place.x);

    document.querySelectorAll('.rest-card').forEach(c => c.classList.remove('active'));
    const activeCard = document.getElementById(`rest-${place.id}`);
    if (activeCard) activeCard.classList.add('active');

    const lat = parseFloat(place.y);
    const lng = parseFloat(place.x);
    const position = new kakao.maps.LatLng(lat, lng);

    mainMap.relayout();
    mainMap.panTo(position);
    mainMap.setLevel(3);

    clearMarkers(mainMarkers);

    const markerSize = new kakao.maps.Size(28, 40);
    const markerImg = new kakao.maps.MarkerImage(BLUE_PIN, markerSize);
    const marker = new kakao.maps.Marker({
        position: position,
        map: mainMap,
        image: markerImg
    });
    mainMarkers.push(marker);

    mainInfoWindow.setContent(`
        <div style="padding: 8px 14px; font-size: 13px; font-weight: bold; color: #202b36; white-space: nowrap; width: max-content; display: inline-block;">
            🍴 ${place.place_name}
        </div>
    `);
    mainInfoWindow.open(mainMap, marker);

    document.getElementById('selected-name').innerText = place.place_name;
    document.getElementById('selected-sub').innerHTML = `
        주소: ${place.road_address_name || place.address_name} | 
        <a href="${place.place_url}" target="_blank" style="color:#2ba6cf; text-decoration:none; font-weight:bold;">🔗 카카오맵 바로가기</a>
    `;

    const addr = place.address_name || place.road_address_name || "";
    if (addr) {
        const newRegion = extractAccurateRegion(addr);
        // 지역명이 실제로 바뀌었을 때만 날씨/추천 다시 갱신 (같은 지역 내 식당 클릭 시에는 유지)
        if (newRegion !== currentSearchRegion) {
            currentSearchRegion = newRegion;
            fetchWeatherForHero(place.y, place.x, currentSearchRegion);
        }
    }

    document.getElementById('open-modal-btn').style.display = "inline-block";
    document.getElementById('open-review-btn').style.display = "inline-block";
}

// 찜 목록 불러오기
function loadFavoriteList() {
    if (!currentLoggedInUser) {
        favoriteList = [];
        return;
    }

    const username = currentLoggedInUser.username;

    fetch(`/api/favorites/${encodeURIComponent(username)}`)
        .then(res => {
            if (!res.ok) throw new Error("찜 목록을 불러오지 못했습니다.");
            return res.json();
        })
        .then(data => {
            favoriteList = (data || []).map(item => ({
                id: item.placeId,
                name: item.placeName,
                address: item.address,
                phone: item.phone,
                x: item.x,
                y: item.y,
                place_url: item.placeUrl,
                category_name: item.categoryName
            }));

            if (currentPlaces.length > 0) renderList(currentPlaces);
        })
        .catch(err => console.error("찜 목록 조회 오류:", err));
}

// 찜 토글
function toggleFavorite(event, place) {
    event.stopPropagation();

    const index = favoriteList.findIndex(item => item.id === place.id);
    const targetBtn = document.getElementById(`fav-btn-${place.id}`);

    if (index > -1) {
        favoriteList.splice(index, 1);
        if (targetBtn) targetBtn.innerText = "🤍";

        if (currentLoggedInUser) {
            const username = currentLoggedInUser.username;
            fetch(`/api/favorites/${encodeURIComponent(username)}/${encodeURIComponent(place.id)}`, {
                method: "DELETE"
            }).catch(err => console.error("찜 삭제 오류:", err));
        }
        return;
    }

    const favorite = {
        id: place.id,
        name: place.place_name,
        address: place.road_address_name || place.address_name,
        phone: place.phone,
        x: place.x,
        y: place.y,
        place_url: place.place_url,
        category_name: place.category_name
    };

    favoriteList.push(favorite);
    if (targetBtn) targetBtn.innerText = "❤️";

    if (!currentLoggedInUser) return;

    fetch("/api/favorites", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            username: currentLoggedInUser.username,
            placeId: place.id,
            placeName: place.place_name,
            address: place.road_address_name || place.address_name,
            phone: place.phone,
            x: place.x,
            y: place.y,
            placeUrl: place.place_url,
            categoryName: place.category_name
        })
    })
        .then(res => {
            if (!res.ok) throw new Error("찜 저장에 실패했습니다.");
        })
        .catch(err => {
            console.error("찜 저장 오류:", err);
            alert(err.message);
        });
}

function openFavoriteModal() {
    const modal = document.getElementById("favorite-modal");
    const content = document.getElementById("favorite-list-content");
    content.innerHTML = "";

    if (favoriteList.length === 0) {
        content.innerHTML = `
            <div style="text-align: center; padding: 40px 0; color: #8293a1;">
                <div style="font-size: 40px; margin-bottom: 10px;">💔</div>
                <p style="font-size: 14px;">아직 찜한 맛집이 없습니다.</p>
            </div>
        `;
    } else {
        favoriteList.forEach((item, index) => {
            const div = document.createElement("div");
            div.className = "fav-item";
            div.innerHTML = `
                <div class="fav-item-info" onclick='clickFavPlace(${JSON.stringify(item)})'>
                    <h5>${index + 1}. ${item.name}</h5>
                    <p>📍 ${item.address}</p>
                    <p>📞 ${item.phone || '전화번호 정보 없음'}</p>
                </div>
                <button class="fav-del-btn" onclick="removeFavorite('${item.id}')" title="찜 취소">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            `;
            content.appendChild(div);
        });
    }
    modal.style.display = "flex";
}

function closeFavoriteModal(event) {
    if (event && event.target !== event.currentTarget && !event.target.closest('.modal-close-btn')) {
        return;
    }
    document.getElementById("favorite-modal").style.display = "none";
}

function removeFavorite(id) {
    const index = favoriteList.findIndex(item => item.id === id);
    if (index > -1) favoriteList.splice(index, 1);

    const targetBtn = document.getElementById(`fav-btn-${id}`);
    if (targetBtn) targetBtn.innerText = "🤍";

    if (currentLoggedInUser) {
        const username = currentLoggedInUser.username;
        fetch(`/api/favorites/${encodeURIComponent(username)}/${encodeURIComponent(id)}`, {
            method: "DELETE"
        }).catch(err => console.error("찜 삭제 오류:", err));
    }

    openFavoriteModal();
}

function clickFavPlace(place) {
    closeFavoriteModal();
    selectPlace({
        id: place.id,
        place_name: place.name,
        road_address_name: place.address,
        address_name: place.address,
        phone: place.phone,
        x: place.x,
        y: place.y,
        place_url: place.place_url,
        category_name: place.category_name || '음식점'
    });
}

// 주변 관광지 모달 로직
async function openTourModal() {
    if (!currentSelectedPlace) return;

    const modal = document.getElementById('tour-modal');
    modal.style.display = "flex";
    document.getElementById('modal-title').innerHTML = `<i class="fa-solid fa-map-location-dot"></i> [${currentSelectedPlace.place_name}] 주변 관광지`;

    const lat = parseFloat(currentSelectedPlace.y);
    const lng = parseFloat(currentSelectedPlace.x);
    const placePos = new kakao.maps.LatLng(lat, lng);

    setTimeout(() => {
        if (!modalMap) {
            const container = document.getElementById('modal-map');
            modalMap = new kakao.maps.Map(container, { center: placePos, level: 5 });
            modalInfoWindow = new kakao.maps.InfoWindow({ zIndex: 10 });
        } else {
            modalMap.relayout();
            modalMap.setCenter(placePos);
            modalMap.setLevel(5);
        }

        clearMarkers(modalMarkers);

        const blueSize = new kakao.maps.Size(30, 42);
        const blueImg = new kakao.maps.MarkerImage(BLUE_PIN, blueSize);
        const restMarker = new kakao.maps.Marker({
            position: placePos,
            map: modalMap,
            image: blueImg
        });
        modalMarkers.push(restMarker);

        modalInfoWindow.setContent(`
            <div style="padding: 8px 14px; font-size: 13px; font-weight: bold; color: #202b36; white-space: nowrap; width: max-content; display: inline-block;">
                🍴 ${currentSelectedPlace.place_name} (선택된 맛집)
            </div>
        `);
        modalInfoWindow.open(modalMap, restMarker);

        fetchModalTourList(lat, lng);
    }, 100);
}

function closeTourModal(event) {
    if (event && event.target !== event.currentTarget && !event.target.closest('.modal-close-btn')) {
        return;
    }
    document.getElementById('tour-modal').style.display = "none";
}

async function fetchModalTourList(lat, lng) {
    const listEl = document.getElementById('modal-tour-list');
    listEl.innerHTML = "<p style='text-align:center; color:#888; padding:30px;'>반경 3km 내 관광명소를 탐색 중입니다...</p>";

    const url = `https://dapi.kakao.com/v2/local/search/category.json?category_group_code=AT4&x=${lng}&y=${lat}&radius=3000&sort=distance&size=10`;

    try {
        const res = await fetch(url, {
            headers: { Authorization: `KakaoAK ${KAKAO_REST_API_KEY}` }
        });
        const data = await res.json();
        currentTourPlaces = data.documents || []; // <-- [추가]
        renderModalTourList(currentTourPlaces);   // <-- data.documents 대신 currentTourPlaces 전달
    } catch (err) {
        console.error(err);
        listEl.innerHTML = "<p style='text-align:center; color:red; padding:30px;'>관광지 정보를 불러오지 못했습니다.</p>";
    }
}

function renderModalTourList(tours) {
    const listEl = document.getElementById('modal-tour-list');
    listEl.innerHTML = "";

    if (!tours || tours.length === 0) {
        listEl.innerHTML = "<p style='text-align:center; color:#888; padding:30px;'>반경 3km 이내에 등록된 관광지가 없습니다.</p>";
        return;
    }

    const yellowSize = new kakao.maps.Size(26, 38);
    const yellowImg = new kakao.maps.MarkerImage(YELLOW_PIN, yellowSize);

    tours.forEach(tour => {
        const tourPos = new kakao.maps.LatLng(parseFloat(tour.y), parseFloat(tour.x));

        const tourMarker = new kakao.maps.Marker({
            position: tourPos,
            map: modalMap,
            image: yellowImg
        });
        modalMarkers.push(tourMarker);

        kakao.maps.event.addListener(tourMarker, 'click', function() {
            modalInfoWindow.setContent(`
                <div style="padding: 6px 12px; font-size: 12px; font-weight: bold; color: #202b36; white-space: nowrap; width: max-content; display: inline-block;">
                    🏞️ ${tour.place_name} (${tour.distance}m)
                </div>
            `);
            modalInfoWindow.open(modalMap, tourMarker);
        });

        const card = document.createElement('div');
        card.className = 'tour-card';
        card.innerHTML = `
		            <span class="tour-card-badge">관광명소</span>
		            <h5>${tour.place_name}</h5>
		            <p>📏 맛집과의 거리: 약 <strong>${tour.distance}m</strong></p>
		            <p>📍 ${tour.road_address_name || tour.address_name}</p>
		           
					<div style="display: flex; align-items: center; justify-content: space-between; margin-top: 8px;">
					                <div class="reaction-wrap">
					                    <button class="reaction-btn like-btn" id="like-btn-${tour.id}" onclick='toggleReaction(event, "${tour.id}", "LIKE")'>
					                        👍 <span id="like-count-${tour.id}" class="reaction-count">0</span>
					                    </button>
					                    <button class="reaction-btn dislike-btn" id="dislike-btn-${tour.id}" onclick='toggleReaction(event, "${tour.id}", "DISLIKE")'>
					                        👎 <span id="dislike-count-${tour.id}" class="reaction-count">0</span>
					                    </button>
					                </div>
					            </div>
					
					 <button onclick='event.stopPropagation(); openTourReviewModal(${JSON.stringify(tour).replace(/'/g, "&#39;")})' 
		                 style="width: 100%; margin-top: 10px; background: linear-gradient(135deg, #f59f00, #e67e22); color: white; border: none; padding: 7px 0; border-radius: 6px; font-weight: bold; font-size: 0.85rem; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 4px;">
		                 📝 리뷰 남기기/보기
		            </button>
		        `;


        if (typeof loadReactions === 'function') {
            loadReactions(tour.id);
        }

        card.onclick = () => {
            modalMap.panTo(tourPos);
            modalInfoWindow.setContent(`
                <div style="padding: 6px 12px; font-size: 12px; font-weight: bold; color: #202b36; white-space: nowrap; width: max-content; display: inline-block;">
                    🏞️ ${tour.place_name} (${tour.distance}m)
                </div>
            `);
            modalInfoWindow.open(modalMap, tourMarker);
        };
        listEl.appendChild(card);
    });
}

function clearMarkers(markerArr) {
    markerArr.forEach(m => m.setMap(null));
    markerArr.length = 0;
}

// 로그인 / 회원가입 모달 로직
function openAuthModal() {
    if (currentLoggedInUser) {
        if (confirm("로그아웃 하시겠습니까?")) {
            currentLoggedInUser = null;
            favoriteList = [];
            sessionStorage.removeItem("currentLoggedInUser");
            document.getElementById("header-auth-btn").innerText = "로그인";
            document.getElementById("my-page-btn").style.display = "none";
            alert("로그아웃되었습니다.");
        }
        return;
    }
    switchAuthTab('login');
    document.getElementById("auth-modal").style.display = "flex";
}

function closeAuthModal(event) {
    if (event && event.target !== event.currentTarget && !event.target.closest('.modal-close-btn')) {
        return;
    }
    document.getElementById("auth-modal").style.display = "none";
}

function switchAuthTab(type) {
    const loginTab = document.getElementById("tab-login");
    const regTab = document.getElementById("tab-register");
    const mainTabs = document.getElementById("auth-main-tabs");

    const loginForm = document.getElementById("login-form-area");
    const regForm = document.getElementById("register-form-area");
    const findIdForm = document.getElementById("find-id-form-area");
    const findPwForm = document.getElementById("find-pw-form-area");
    const resetPwForm = document.getElementById("reset-pw-form-area");
    const title = document.getElementById("auth-modal-title");

    if (loginForm) loginForm.style.display = "none";
    if (regForm) regForm.style.display = "none";
    if (findIdForm) findIdForm.style.display = "none";
    if (findPwForm) findPwForm.style.display = "none";
    if (resetPwForm) resetPwForm.style.display = "none";

    if (type === 'login') {
        if (mainTabs) mainTabs.style.display = "flex";
        loginTab.classList.add("active");
        regTab.classList.remove("active");
        loginForm.style.display = "flex";
        title.innerHTML = '<i class="fa-solid fa-user-lock"></i> 로그인';
    } else if (type === 'register') {
        if (mainTabs) mainTabs.style.display = "flex";
        regTab.classList.add("active");
        loginTab.classList.remove("active");
        regForm.style.display = "flex";
        title.innerHTML = '<i class="fa-solid fa-user-plus"></i> 회원가입';
    } else if (type === 'find-id') {
        if (mainTabs) mainTabs.style.display = "none";
        findIdForm.style.display = "flex";
        title.innerHTML = '<i class="fa-solid fa-magnifying-glass"></i> 아이디 찾기';
    } else if (type === 'find-pw') {
        if (mainTabs) mainTabs.style.display = "none";
        findPwForm.style.display = "flex";
        title.innerHTML = '<i class="fa-solid fa-key"></i> 비밀번호 찾기';
    } else if (type === 'reset-pw') {
        if (mainTabs) mainTabs.style.display = "none";
        resetPwForm.style.display = "flex";
        title.innerHTML = '<i class="fa-solid fa-lock-open"></i> 비밀번호 재설정';
    }
}

// =========================================================
// [추가] 아이디 찾기 요청
// =========================================================
function requestFindId() {
    const nickname = document.getElementById("find-id-nickname").value.trim();
    const question = document.getElementById("find-id-question").value;
    const answer = document.getElementById("find-id-answer").value.trim();

    if (!nickname || !answer) {
        alert("닉네임과 보안 질문 답변을 모두 입력해주세요.");
        return;
    }

    fetch('/api/member/find-id', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            nickname: nickname,
            securityQuestion: question,
            securityAnswer: answer
        })
    })
        .then(async res => {
            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg);
            }
            return res.json();
        })
        .then(data => {
            // 회원님의 아이디 안내 팝업 (확인 / 취소)
            const confirmLogin = confirm(`회원님의 아이디는 [ ${data.username} ] 입니다.`);

            if (confirmLogin) {
                // 입력 필드 초기화
                document.getElementById("find-id-nickname").value = "";
                document.getElementById("find-id-answer").value = "";

                // 로그인 탭으로 전환 및 아이디 자동 완성
                switchAuthTab('login');
                document.getElementById("login-id").value = data.username;
                document.getElementById("login-pw").focus();
            }
        })
        .catch(err => {
            alert(err.message || "일치하는 회원 정보를 찾을 수 없습니다.");
        });
}

let foundUserPasswordTemp = "";

function requestFindPw() {
    const id = document.getElementById("find-pw-id").value.trim();
    const question = document.getElementById("find-pw-question").value;
    const answer = document.getElementById("find-pw-answer").value.trim();

    if (!id || !answer) {
        alert("아이디와 보안 질문 답변을 모두 입력해주세요.");
        return;
    }

    fetch('/api/member/find-pw', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            username: id,
            securityQuestion: question,
            securityAnswer: answer
        })
    })
        .then(async res => {
            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg);
            }
            return res.json();
        })
        .then(data => {
            foundUserPasswordTemp = data.password;

            const confirmReset = confirm(
                `${id} 회원님의 비밀번호는 [ ${data.password} ] 입니다.\n\n비밀번호 변경을 권장합니다.\n지금 비밀번호를 변경하시겠습니까?`
            );

            if (confirmReset) {
                document.getElementById("reset-pw-id").value = id;
                document.getElementById("reset-pw-current").value = "";
                document.getElementById("reset-pw-new").value = "";
                document.getElementById("reset-pw-confirm").value = "";
                switchAuthTab('reset-pw');
            } else {
                foundUserPasswordTemp = "";
                document.getElementById("find-pw-id").value = "";
                document.getElementById("find-pw-answer").value = "";
                switchAuthTab('login');
            }
        })
        .catch(err => {
            alert(err.message || "일치하는 회원 정보를 찾을 수 없습니다.");
        });
}

function requestDirectResetPw() {
    const id = document.getElementById("reset-pw-id").value.trim();
    const currentPw = document.getElementById("reset-pw-current").value.trim();
    const newPw = document.getElementById("reset-pw-new").value.trim();
    const confirmPw = document.getElementById("reset-pw-confirm").value.trim();

    if (!currentPw || !newPw || !confirmPw) {
        alert("모든 입력란을 채워주세요.");
        return;
    }

    if (currentPw !== foundUserPasswordTemp) {
        alert("현재 비밀번호가 일치하지 않습니다. 다시 확인해주세요.");
        return;
    }

    if (currentPw === newPw) {
        alert("새로운 비밀번호는 기존 비밀번호와 다르게 설정해야 합니다.");
        return;
    }

    if (newPw !== confirmPw) {
        alert("새로운 비밀번호가 일치하지 않습니다. 다시 확인해주세요.");
        return;
    }

    const pwRegex = /^[a-zA-Z0-9]{4,16}$/;
    if (!pwRegex.test(newPw)) {
        alert("새 비밀번호는 영문 또는 숫자 4~16자리로 입력해주세요.");
        return;
    }

    fetch('/api/member/' + encodeURIComponent(id), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            username: id,
            password: newPw
        })
    })
        .then(async res => {
            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg);
            }
            return res.text();
        })
        .then(() => {
            alert("비밀번호가 성공적으로 변경되었습니다. 새로운 비밀번호로 로그인해주세요!");
            foundUserPasswordTemp = "";
            document.getElementById("reset-pw-id").value = "";
            document.getElementById("reset-pw-current").value = "";
            document.getElementById("reset-pw-new").value = "";
            document.getElementById("reset-pw-confirm").value = "";
            switchAuthTab('login');
        })
        .catch(err => {
            alert(err.message || "비밀번호 변경에 실패했습니다.");
        });
}

function requestLogin() {
    const id = document.getElementById("login-id").value.trim();
    const pw = document.getElementById("login-pw").value.trim();

    if (!id || !pw) {
        alert("아이디와 비밀번호를 모두 입력해주세요.");
        return;
    }

    fetch('/api/member/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: id, password: pw })
    })
        .then(res => {
            if (!res.ok) throw new Error();
            return res.text();
        })
        .then(() => {
            fetch(`/api/member/${encodeURIComponent(id)}`)
                .then(res => {
                    if (!res.ok) throw new Error();
                    return res.json();
                })
                .then(member => {
                    currentLoggedInUser = {
                        id: id,
                        username: id,
                        nickname: member.nickname
                    };
                    sessionStorage.setItem("currentLoggedInUser", JSON.stringify(currentLoggedInUser));
                    document.getElementById("header-auth-btn").innerText = "로그아웃";
                    document.getElementById("my-page-btn").style.display = "flex";
                    alert(`${member.nickname}님, 환영합니다!`);
                    document.getElementById("login-id").value = "";
                    document.getElementById("login-pw").value = "";
                    closeAuthModal();
                });
        })
        .catch(() => alert("아이디 또는 비밀번호를 다시 확인해주세요."));
}

function requestRegister() {
    const nickname = document.getElementById("reg-nickname").value.trim();
    const id = document.getElementById("reg-id").value.trim();
    const pw = document.getElementById("reg-pw").value.trim();

    const questionEl = document.getElementById("reg-question");
    const answerEl = document.getElementById("reg-answer");
    const question = questionEl ? questionEl.value : "";
    const answer = answerEl ? answerEl.value.trim() : "";

    if (!nickname || !id || !pw || !answer) {
        alert("닉네임, 아이디, 비밀번호, 보안 질문 답변을 모두 입력해주세요.");
        return;
    }

    const idRegex = /^[a-zA-Z0-9]{4,12}$/;
    if (!idRegex.test(id)) {
        alert("아이디는 영문 또는 숫자 4~12자리로 입력해주세요.");
        return;
    }

    const pwRegex = /^[a-zA-Z0-9]{4,16}$/;
    if (!pwRegex.test(pw)) {
        alert("비밀번호는 영문 또는 숫자 4~16자리로 입력해주세요.");
        return;
    }

    const nickRegex = /^[a-zA-Z0-9가-힣]{2,20}$/;
    if (!nickRegex.test(nickname)) {
        alert("닉네임은 한글, 영문, 숫자 2~20자리로 입력해주세요.");
        return;
    }

    fetch('/api/member/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            nickname: nickname,
            username: id,
            password: pw,
            securityQuestion: question,
            securityAnswer: answer
        })
    })
        .then(async res => {
            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg);
            }
            return res.text();
        })
        .then(() => {
            alert("회원가입이 완료되었습니다! 로그인해 주세요.");
            document.getElementById("reg-nickname").value = "";
            document.getElementById("reg-id").value = "";
            document.getElementById("reg-pw").value = "";
            if (answerEl) answerEl.value = "";
            switchAuthTab('login');
        })
        .catch(err => {
            alert(err.message || "회원가입 중 오류가 발생했습니다.");
        });
}

// 날씨 및 추천 음식 로직
async function fetchWeatherForHero(lat, lng, regionName) {
    const url = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lng}&appid=${OPENWEATHER_API_KEY}&units=metric&lang=kr`;

    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error();
        const data = await res.json();

        const temp = Math.round(data.main.temp);
        const weatherMain = data.weather[0].main;
        const iconCode = data.weather[0].icon;

        document.getElementById("weather-title").innerText = `지금 ${regionName} (${temp}°C)`;
        document.getElementById("weather-icon-area").innerHTML = `<img src="https://openweathermap.org/img/wn/${iconCode}.png" style="width: 44px; height: 44px;">`;

        let descText = "";
        let foodCandidates = [];

        if (weatherMain === "Rain" || weatherMain === "Drizzle" || weatherMain === "Thunderstorm") {
            descText = "비 내리는 날엔<br><strong>따끈 바삭한 메뉴! ☔</strong>";
            foodCandidates = ["전집", "칼국수", "수제비", "짬뽕", "막걸리", "파전"];
        } else if (weatherMain === "Snow") {
            descText = "눈 오는 날엔<br><strong>몸을 녹여줄 국물! ❄️</strong>";
            foodCandidates = ["만두전골", "설렁탕", "우동", "부대찌개", "샤브샤브"];
        } else if (temp >= 28) {
            descText = "무더운 날씨엔<br><strong>가슴속까지 시원하게! 🧊</strong>";
            foodCandidates = ["냉면", "콩국수", "물회", "막국수", "빙수", "소바"];
        } else if (temp >= 20 && weatherMain === "Clear") {
            descText = "화창하고 따뜻한 날<br><strong>기분 좋은 외식! ☀️</strong>";
            foodCandidates = ["야외 브런치", "화덕피자", "수제버거", "파스타", "루프탑 카페"];
        } else if (temp <= 5) {
            descText = "쌀쌀한 추위엔<br><strong>진하고 뜨끈한 국물! 🍲</strong>";
            foodCandidates = ["순대국밥", "감자탕", "갈비탕", "해장국", "어묵탕", "라멘"];
        } else if (weatherMain === "Clouds") {
            descText = "흐린 날씨엔<br><strong>얼큰하고 든든한 한 끼! ☁️</strong>";
            foodCandidates = ["김치찌개", "닭볶음탕", "곱창전골", "떡볶이", "마라탕"];
        } else {
            descText = "오늘 같은 날엔<br><strong>실패 없는 인기 메뉴! 🍴</strong>";
            foodCandidates = ["삼겹살", "돈까스", "초밥", "돼지갈비", "보쌈", "베트남쌀국수"];
        }

        // [수정]: 지역이 바뀌었거나 최초 1회 실행일 때만 랜덤 추출, 같은 지역이면 이전 메뉴 유지
        if (lastRecommendedRegion !== regionName || !currentRecommendedFood) {
            currentRecommendedFood = foodCandidates[Math.floor(Math.random() * foodCandidates.length)];
            lastRecommendedRegion = regionName;
        }

        currentWeatherRecommendFood = currentRecommendedFood;
        document.getElementById("weather-recommend-text").innerHTML = descText;
        document.getElementById("weather-recommend-btn").innerText = `추천 ${currentWeatherRecommendFood} 보기`;

    } catch (err) {
        console.error("날씨 정보 조회 실패:", err);
    }
}

function searchRecommendFood() {
    const targetQuery = `${currentSearchRegion} ${currentWeatherRecommendFood}`;
    document.getElementById("search-input").value = targetQuery;
    fetchRestaurants(targetQuery);
}

// [맛집 전용] 리뷰 모달 열기
function openReviewModal() {
    if (!currentSelectedPlace) {
        alert("장소를 먼저 선택해주세요.");
        return;
    }

    currentReviewType = 'RESTAURANT';
    currentSelectedTourSpot = null;

    const modal = document.getElementById("review-modal");
    if (!modal) return;

    const titleEl = document.getElementById("review-modal-title");
    if (titleEl) {
        titleEl.innerHTML = `<i class="fa-solid fa-comments"></i> [${currentSelectedPlace.place_name}] 맛집 리뷰`;
    }

    const writeSec = document.getElementById("review-write-section");
    const alertSec = document.getElementById("review-login-alert");

    if (currentLoggedInUser) {
        if (writeSec) writeSec.style.display = "block";
        if (alertSec) alertSec.style.display = "none";
    } else {
        if (writeSec) writeSec.style.display = "none";
        if (alertSec) alertSec.style.display = "block";
    }

    modal.style.display = "flex";
    loadRestaurantReviews();
}

// [관광지 전용] 리뷰 모달 열기 (신규)
function openTourReviewModal(tour) {
    currentReviewType = 'TOUR';
    currentSelectedTourSpot = tour; // 관광지 정보만 따로 보관 (맛집 변수 보호)

    const modal = document.getElementById("review-modal");
    if (!modal) return;

    const titleEl = document.getElementById("review-modal-title");
    if (titleEl) {
        titleEl.innerHTML = `<i class="fa-solid fa-camera"></i> [${tour.place_name}] 관광지 리뷰`;
    }

    const writeSec = document.getElementById("review-write-section");
    const alertSec = document.getElementById("review-login-alert");

    if (currentLoggedInUser) {
        if (writeSec) writeSec.style.display = "block";
        if (alertSec) alertSec.style.display = "none";
    } else {
        if (writeSec) writeSec.style.display = "none";
        if (alertSec) alertSec.style.display = "block";
    }

    modal.style.display = "flex";
    loadTourReviews();
}

function closeReviewModal(event) {
    if (event && event.target !== event.currentTarget && !event.target.closest('.modal-close-btn')) {
        return;
    }
    document.getElementById("review-modal").style.display = "none";
}

function isCurrentPlaceTour() {
    if (!currentSelectedPlace) return false;
    if (currentSelectedPlace.category_group_code !== 'FD6' && currentSelectedPlace.category_group_code !== 'CE7') {
        return true;
    }
    if (currentSelectedPlace.isTour === true) return true;
    if (currentSelectedPlace.category_group_code === 'AT4' || currentSelectedPlace.category_group_code === 'CT1') {
        return true;
    }
    const catName = currentSelectedPlace.category_name || '';
    if (catName.includes('관광') || catName.includes('명소') || catName.includes('여행') || catName.includes('문화')) {
        return true;
    }
    return false;
}

function loadRestaurantReviews() {
    const container = document.getElementById("review-list-container") || document.getElementById("reviewList");
    if (!container) return;

    container.innerHTML = `<p style="text-align: center; color: #888; font-size: 0.85rem;">리뷰를 불러오는 중입니다...</p>`;
    const placeId = currentSelectedPlace.id;

    fetch("/api/reviews/place/" + encodeURIComponent(placeId))
        .then(res => {
            if (!res.ok) throw new Error("리뷰를 불러오지 못했습니다.");
            return res.json();
        })
        .then(data => {
            container.innerHTML = "";
            if (!data || data.length === 0) {
                container.innerHTML = `<p style="text-align: center; color: #888; font-size: 0.85rem;">등록된 리뷰가 없습니다. 첫 리뷰를 남겨보세요!</p>`;
                return;
            }

            data.slice().reverse().forEach(rev => {
                const isDeletedUser = rev.username === "탈퇴한 회원";
                const userDisplayName = isDeletedUser ? "(탈퇴한 회원)" : rev.username;
                const userColor = isDeletedUser ? "#94a3b8" : "#1e293b";

                const div = document.createElement("div");
                div.style.cssText = "background: #f1f5f9; padding: 10px 14px; border-radius: 8px; font-size: 0.88rem; color: #334155; line-height: 1.4; margin-bottom: 6px;";
                div.innerHTML = `
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                        <strong style="color: ${userColor}; font-weight: ${isDeletedUser ? 'normal' : 'bold'};">💬 ${userDisplayName}</strong>
                    </div>
                    <p style="margin: 0; color: #475569;">${rev.content}</p>
                `;
                container.appendChild(div);
            });
        })
        .catch(err => {
            console.error("맛집 리뷰 조회 오류:", err);
            container.innerHTML = `<p style="text-align: center; color: red; font-size: 0.85rem;">리뷰를 불러오지 못했습니다.</p>`;
        });
}

function submitRestaurantReview() {
    const input = document.getElementById("review-content-input") || document.getElementById("reviewContent");
    if (!input) return;

    const content = input.value.trim();
    if (!content) return alert("리뷰 내용을 입력해주세요.");
    if (!currentLoggedInUser) return alert("로그인 후 리뷰를 작성해주세요.");
    if (!currentSelectedPlace) return alert("장소를 먼저 선택해주세요.");

    fetch("/api/reviews", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            content: content,
            username: currentLoggedInUser.username,
            placeId: currentSelectedPlace.id,
            restaurantName: currentSelectedPlace.place_name,
            restaurantAddress: currentSelectedPlace.road_address_name || currentSelectedPlace.address_name,
            restaurantUrl: currentSelectedPlace.place_url
        })
    })
        .then(async res => {
            if (!res.ok) throw new Error(await res.text());
            return res.text();
        })
        .then(() => {
            alert("리뷰가 정상적으로 등록되었습니다!");
            input.value = "";
            loadRestaurantReviews();
        })
        .catch(err => {
            alert(err.message || "리뷰 등록 중 오류가 발생했습니다.");
        });
}

// 관광지 리뷰 조회
async function loadTourReviews() {
    if (!currentSelectedTourSpot) return;
    const spotId = currentSelectedTourSpot.id;
    const reviewListEl = document.getElementById("review-list-container");

    if (!reviewListEl) return;
    reviewListEl.innerHTML = "<p style='text-align:center; color:#888; padding:20px;'>리뷰를 불러오는 중입니다...</p>";

    try {
        const response = await fetch(`/api/tour-reviews?spotId=${encodeURIComponent(spotId)}`);
        if (!response.ok) throw new Error("리뷰 로드 실패");

        const reviews = await response.json();
        renderTourReviewList(reviews);
    } catch (error) {
        console.error("관광지 리뷰 로드 오류:", error);
        reviewListEl.innerHTML = "<p style='text-align:center; color:#e74c3c; padding:20px;'>리뷰를 불러오지 못했습니다.</p>";
    }
}

// 관광지 리뷰 화면 렌더링 (날짜 파싱 및 안전한 텍스트 출력)
function renderTourReviewList(reviews) {
    const reviewListEl = document.getElementById("review-list-container");
    if (!reviewListEl) return;

    reviewListEl.innerHTML = "";

    if (!reviews || reviews.length === 0) {
        reviewListEl.innerHTML = "<p style='text-align:center; color:#888; padding:20px;'>아직 작성된 리뷰가 없습니다. 첫 리뷰를 남겨보세요!</p>";
        return;
    }

    reviews.forEach(review => {
        let dateDisplay = "";
        if (review.createdAt) {
            if (Array.isArray(review.createdAt)) {
                dateDisplay = `${review.createdAt[0]}.${String(review.createdAt[1]).padStart(2, '0')}.${String(review.createdAt[2]).padStart(2, '0')} ${String(review.createdAt[3] || 0).padStart(2, '0')}:${String(review.createdAt[4] || 0).padStart(2, '0')}`;
            } else {
                dateDisplay = String(review.createdAt).replace('T', ' ').substring(0, 16);
            }
        }

        const item = document.createElement('div');
        item.className = 'review-item';
        item.style.cssText = "background: #f8fafc; border-radius: 8px; padding: 12px; margin-bottom: 8px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 4px;";

        const writerName = review.writer || '익명';
        const reviewContent = review.content || '';

        item.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <strong style="color: #1e293b; font-size: 0.88rem;">💬 ${writerName}</strong>
                <span style="font-size: 0.75rem; color: #94a3b8;">${dateDisplay}</span>
            </div>
            <p style="margin: 4px 0 0 0; font-size: 0.88rem; color: #475569; line-height: 1.4; word-break: break-all;"></p>
        `;

        item.querySelector('p').textContent = reviewContent;
        reviewListEl.appendChild(item);
    });
}

// 관광지 리뷰 제출
async function handleTourReviewSubmit(event) {
    if (event) event.preventDefault();

    const contentInput = document.getElementById("review-content-input");
    const content = contentInput ? contentInput.value.trim() : "";

    if (!content) return alert("리뷰 내용을 입력해주세요.");
    if (!currentLoggedInUser) return alert("로그인 후 리뷰를 작성해주세요.");
    if (!currentSelectedTourSpot) return alert("관광지 정보가 없습니다.");

    const spotId = currentSelectedTourSpot.id;
    const spotName = currentSelectedTourSpot.place_name;
    const writer = currentLoggedInUser.nickname || currentLoggedInUser.username || "익명";

    try {
        const response = await fetch('/api/tour-reviews', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                spotId: spotId,
                spotName: spotName,
                content: content,
                writer: writer
            })
        });

        if (!response.ok) {
            const errData = await response.json().catch(() => ({}));
            throw new Error(errData.error || "비속어나 부적절한 표현이 포함되어 등록할 수 없습니다.");
        }

        alert("관광지 리뷰가 등록되었습니다!");
        if (contentInput) contentInput.value = '';
        loadTourReviews(); // 관광지 리뷰 목록 새로고침

    } catch (error) {
        console.error("관광지 리뷰 제출 오류:", error);
        alert(error.message || "서버 통신 중 오류가 발생했습니다.");
    }
}

// [리뷰 등록 버튼 클릭 시 최종 실행되는 함수]
function submitReview(event) {
    if (currentReviewType === 'TOUR') {
        handleTourReviewSubmit(event);
    } else {
        submitRestaurantReview();
    }
}

function loadReactions(placeId) {
    const username = currentLoggedInUser ? currentLoggedInUser.username : "";
    let url = "/api/reactions/" + encodeURIComponent(placeId);

    if (username) {
        url += "?username=" + encodeURIComponent(username);
    }

    fetch(url)
        .then(res => {
            if (!res.ok) throw new Error("반응 정보를 불러오지 못했습니다.");
            return res.json();
        })
        .then(data => {
            const likeCount = document.getElementById(`like-count-${placeId}`);
            const dislikeCount = document.getElementById(`dislike-count-${placeId}`);
            const likeBtn = document.getElementById(`like-btn-${placeId}`);
            const dislikeBtn = document.getElementById(`dislike-btn-${placeId}`);

            if (likeCount) likeCount.innerText = data.likeCount || 0;
            if (dislikeCount) dislikeCount.innerText = data.dislikeCount || 0;

            if (likeBtn) likeBtn.classList.remove("active");
            if (dislikeBtn) dislikeBtn.classList.remove("active");

            if (data.userReaction === "LIKE" && likeBtn) {
                likeBtn.classList.add("active");
            }
            if (data.userReaction === "DISLIKE" && dislikeBtn) {
                dislikeBtn.classList.add("active");
            }
        })
        .catch(err => {
            console.error("좋아요/싫어요 조회 오류:", err);
        });
}

function toggleReaction(event, placeId, reactionType) {
    if (event) event.stopPropagation();

    if (!currentLoggedInUser) {
        alert("좋아요/싫어요 기능은 로그인 후 이용할 수 있습니다.");
        return;
    }

    let place = null;

    // 1) 맛집 목록에서 찾기
    if (typeof currentPlaces !== 'undefined' && currentPlaces) {
        place = currentPlaces.find(p => String(p.id) === String(placeId));
    }

    // 2) [추가] 관광지 목록에서도 찾기
    if (!place && typeof currentTourPlaces !== 'undefined' && currentTourPlaces) {
        place = currentTourPlaces.find(p => String(p.id) === String(placeId));
    }

    // 3) 선택된 장소 확인
    if (!place && currentSelectedPlace && String(currentSelectedPlace.id) === String(placeId)) {
        place = currentSelectedPlace;
    }
    if (!place && currentSelectedTourSpot && String(currentSelectedTourSpot.id) === String(placeId)) {
        place = currentSelectedTourSpot;
    }

    if (!place) {
        alert("장소 정보를 찾을 수 없습니다.");
        return;
    }

    const payload = {
        placeId: place.id || placeId,
        username: currentLoggedInUser.username,
        reactionType: reactionType,
        restaurantName: place.place_name || place.spotName || "",
        restaurantAddress: place.road_address_name || place.address_name || "",
        restaurantUrl: place.place_url || ""
    };

    fetch("/api/reactions", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    })
        .then(async res => {
            if (!res.ok) {
                const message = await res.text();
                throw new Error(message);
            }
            return res.json();
        })
        .then(data => {
            const likeCount = document.getElementById(`like-count-${placeId}`);
            const dislikeCount = document.getElementById(`dislike-count-${placeId}`);
            const likeBtn = document.getElementById(`like-btn-${placeId}`);
            const dislikeBtn = document.getElementById(`dislike-btn-${placeId}`);

            if (likeCount) likeCount.innerText = data.likeCount || 0;
            if (dislikeCount) dislikeCount.innerText = data.dislikeCount || 0;

            if (likeBtn) likeBtn.classList.remove("active");
            if (dislikeBtn) dislikeBtn.classList.remove("active");

            if (data.userReaction === "LIKE" && likeBtn) {
                likeBtn.classList.add("active");
            }
            if (data.userReaction === "DISLIKE" && dislikeBtn) {
                dislikeBtn.classList.add("active");
            }
        })
        .catch(err => {
            console.error("좋아요/싫어요 오류:", err);
            alert(err.message || "좋아요/싫어요 처리 중 오류가 발생했습니다.");
        });
}

// AI 챗봇 로직
function openAiChatbot() {
    document.getElementById("ai-chat-panel").style.display = "flex";
    document.getElementById("ai-chat-input").focus();
}

function closeAiChatbot() {
    document.getElementById("ai-chat-panel").style.display = "none";
}

async function sendAiMessage() {
    const input = document.getElementById("ai-chat-input");
    const query = input.value.trim();
    if (!query) return;

    const chatBox = document.getElementById("ai-chat-messages");

    const myMsg = document.createElement("div");
    myMsg.style.cssText = "align-self: flex-end; background: #e0f2fe; color: #0369a1; padding: 10px 14px; border-radius: 12px; max-width: 85%; word-break: break-word;";
    myMsg.innerText = query;
    chatBox.appendChild(myMsg);

    input.value = "";
    chatBox.scrollTop = chatBox.scrollHeight;

    const loadingMsg = document.createElement("div");
    loadingMsg.style.cssText = "align-self: flex-start; background: white; border: 1px solid #e2e8f0; padding: 10px 14px; border-radius: 12px; color: #94a3b8;";
    loadingMsg.innerText = "답변을 생각하는 중입니다... 🤔";
    chatBox.appendChild(loadingMsg);
    chatBox.scrollTop = chatBox.scrollHeight;

    try {
        const res = await fetch("/api/ai/ask", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: query })
        });

        if (!res.ok) throw new Error();
        const data = await res.json();

        loadingMsg.innerHTML = data.reply.replace(/\n/g, "<br>");
        loadingMsg.style.color = "#334155";
    } catch (err) {
        loadingMsg.innerText = "상담 연결이 원활하지 않습니다. 잠시 후 다시 시도해주세요.";
        loadingMsg.style.color = "#ef4444";
    }

    chatBox.scrollTop = chatBox.scrollHeight;
}
// =========================================================
// 🎵 맛따라 여행따라 - YouTube 여행 플레이리스트
// =========================================================

// ⚠️ 여기에 본인의 YouTube Data API v3 키를 입력하세요.
const YOUTUBE_API_KEY = "AIzaSyBFWqyzd0ywGKpGhSXpX-q0hJabCaJCcxo";

// 오늘 날짜를 기준으로 매일 다른 검색어 사용
const TRAVEL_PLAYLIST_KEYWORDS = [
    "신나는 여행 플레이리스트",
    "신나는 드라이브 음악 플레이리스트",
    "여름 여행 신나는 노래",
    "신나는 KPOP 플레이리스트",
    "바다 여행 음악 플레이리스트",
    "기분 좋은 여행 노래",
    "신나는 국내 여행 음악"
];

let currentTravelPlaylistId = "";
let currentTravelPlaylistUrl = "";


/* =========================================================
   플레이리스트 버튼 열기 / 닫기
   ========================================================= */

function toggleTravelPlaylist() {

    const panel = document.getElementById("travel-playlist-panel");

    if (!panel) return;

    panel.classList.toggle("active");

    // 처음 열었을 때만 불러오기
    if (panel.classList.contains("active")) {

        const iframe = document.getElementById("youtube-playlist-player");

        if (!iframe.src || iframe.src === window.location.href) {
            loadTodayTravelPlaylist();
        }
    }
}


/* =========================================================
   오늘의 검색어 결정
   ========================================================= */

function getTodayPlaylistKeyword() {

    const today = new Date();

    const dateNumber =
        today.getFullYear() * 10000 +
        (today.getMonth() + 1) * 100 +
        today.getDate();

    const index =
        dateNumber % TRAVEL_PLAYLIST_KEYWORDS.length;

    return TRAVEL_PLAYLIST_KEYWORDS[index];
}


/* =========================================================
   오늘의 플레이리스트 불러오기
   ========================================================= */

async function loadTodayTravelPlaylist() {

    const loading = document.getElementById("playlist-loading");
    const iframe = document.getElementById("youtube-playlist-player");
    const playlistName = document.getElementById("playlist-name");
    const playlistChannel = document.getElementById("playlist-channel");
    const trackList = document.getElementById("playlist-track-list");

    if (!loading || !iframe) return;

    // API 키가 입력되지 않은 경우
    if (
        !YOUTUBE_API_KEY ||
        YOUTUBE_API_KEY === "YOUR_YOUTUBE_API_KEY"
    ) {

        loading.innerHTML = `
            <i class="fa-solid fa-key"></i>
            <span>YouTube API 키를 입력해주세요.</span>
        `;

        playlistName.innerText = "YouTube API 연결 필요";
        playlistChannel.innerText =
            "map.js의 YOUTUBE_API_KEY에 키를 입력하세요.";

        return;
    }

    loading.style.display = "flex";

    playlistName.innerText = "오늘의 플레이리스트를 찾는 중...";
    playlistChannel.innerText = "YouTube에서 여행 음악을 검색하고 있어요.";

    trackList.innerHTML = `
        <div class="playlist-empty">
            음악 목록을 불러오는 중...
        </div>
    `;

    try {

        const keyword = getTodayPlaylistKeyword();

        /*
         * 오늘 날짜에 이미 검색했던 플레이리스트가 있으면
         * 다시 API 검색을 하지 않음
         */
        const savedDate =
            localStorage.getItem("travelPlaylistDate");

        const savedPlaylist =
            localStorage.getItem("travelPlaylistData");

        let playlistData = null;

        if (
            savedDate === getTodayDateString() &&
            savedPlaylist
        ) {

            try {
                playlistData = JSON.parse(savedPlaylist);
            } catch (e) {
                playlistData = null;
            }
        }


        /*
         * 저장된 오늘의 플레이리스트가 없다면
         * YouTube에서 새로 검색
         */
        if (!playlistData) {

            const searchUrl =
                "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet" +
                "&q=" + encodeURIComponent(keyword) +
                "&type=playlist" +
                "&maxResults=10" +
                "&relevanceLanguage=ko" +
                "&key=" + encodeURIComponent(YOUTUBE_API_KEY);

            const searchResponse =
                await fetch(searchUrl);

            if (!searchResponse.ok) {

                const errorText =
                    await searchResponse.text();

                throw new Error(
                    "YouTube 검색 오류: " + errorText
                );
            }

            const searchData =
                await searchResponse.json();

            if (
                !searchData.items ||
                searchData.items.length === 0
            ) {
                throw new Error(
                    "추천할 플레이리스트를 찾지 못했습니다."
                );
            }


            /*
             * 검색 결과 중 하나를 선택
             *
             * 매일 검색어가 달라지기 때문에
             * 날짜별로 다른 결과를 선택할 수 있음
             */
            const todayNumber =
                Number(
                    getTodayDateString().replace(/-/g, "")
                );

            const selectedIndex =
                todayNumber % searchData.items.length;

            const selected =
                searchData.items[selectedIndex];


            playlistData = {
                playlistId:
                    selected.id.playlistId,

                title:
                    selected.snippet.title,

                channelTitle:
                    selected.snippet.channelTitle,

                thumbnail:
                    selected.snippet.thumbnails?.medium?.url ||
                    selected.snippet.thumbnails?.default?.url ||
                    "",

                keyword: keyword
            };


            /*
             * 오늘 하루 동안 같은 플레이리스트 사용
             */
            localStorage.setItem(
                "travelPlaylistDate",
                getTodayDateString()
            );

            localStorage.setItem(
                "travelPlaylistData",
                JSON.stringify(playlistData)
            );
        }


        // 현재 플레이리스트 저장
        currentTravelPlaylistId =
            playlistData.playlistId;

        currentTravelPlaylistUrl =
            "https://www.youtube.com/playlist?list=" +
            encodeURIComponent(
                currentTravelPlaylistId
            );


        // 정보 표시
        playlistName.innerText =
            playlistData.title;

        playlistChannel.innerText =
            playlistData.channelTitle +
            " · " +
            playlistData.keyword;


        /*
         * YouTube 플레이리스트를 iframe으로 재생
         */
        iframe.src =
            "https://www.youtube.com/embed/videoseries" +
            "?list=" +
            encodeURIComponent(
                currentTravelPlaylistId
            ) +
            "&rel=0";


        /*
         * 곡 목록 가져오기
         */
        await loadTravelPlaylistTracks(
            currentTravelPlaylistId
        );


        loading.style.display = "none";

    } catch (error) {

        console.error(
            "여행 플레이리스트 오류:",
            error
        );

        loading.innerHTML = `
            <i class="fa-solid fa-triangle-exclamation"></i>
            <span>플레이리스트를 불러오지 못했습니다.</span>
        `;

        playlistName.innerText =
            "플레이리스트를 불러오지 못했어요.";

        playlistChannel.innerText =
            "잠시 후 다시 시도해주세요.";

        trackList.innerHTML = `
            <div class="playlist-empty">
                YouTube API 연결을 확인해주세요.
            </div>
        `;
    }
}


/* =========================================================
   오늘 날짜
   ========================================================= */

function getTodayDateString() {

    const today = new Date();

    const year =
        today.getFullYear();

    const month =
        String(today.getMonth() + 1)
            .padStart(2, "0");

    const day =
        String(today.getDate())
            .padStart(2, "0");

    return `${year}-${month}-${day}`;
}


/* =========================================================
   플레이리스트 곡 목록
   ========================================================= */

async function loadTravelPlaylistTracks(
    playlistId
) {

    const trackList =
        document.getElementById(
            "playlist-track-list"
        );

    const trackCount =
        document.getElementById(
            "playlist-track-count"
        );

    if (!trackList) return;


    try {

        const url =
            "https://www.googleapis.com/youtube/v3/playlistItems" +
            "?part=snippet" +
            "&playlistId=" +
            encodeURIComponent(playlistId) +
            "&maxResults=10" +
            "&key=" +
            encodeURIComponent(YOUTUBE_API_KEY);


        const response =
            await fetch(url);


        if (!response.ok) {

            const errorText =
                await response.text();

            throw new Error(
                "곡 목록 오류: " + errorText
            );
        }


        const data =
            await response.json();


        if (
            !data.items ||
            data.items.length === 0
        ) {

            trackList.innerHTML = `
                <div class="playlist-empty">
                    플레이리스트에 등록된 음악이 없습니다.
                </div>
            `;

            if (trackCount) {
                trackCount.innerText = "0곡";
            }

            return;
        }


        trackList.innerHTML = "";


        const validTracks =
            data.items.filter(item => {

                return (
                    item.snippet &&
                    item.snippet.title &&
                    item.snippet.title !== "Private video" &&
                    item.snippet.title !== "Deleted video"
                );

            });


        if (trackCount) {

            trackCount.innerText =
                validTracks.length + "곡";
        }


        validTracks.forEach(
            (item, index) => {

                const snippet =
                    item.snippet;

                const videoId =
                    snippet.resourceId?.videoId;


                const track =
                    document.createElement("div");

                track.className =
                    "playlist-track-item";


                track.innerHTML = `

                    <div class="playlist-track-number">
                        ${index + 1}
                    </div>

                    <img
                        class="playlist-track-thumb"
                        src="${snippet.thumbnails?.default?.url ||
                    ""
                    }"
                        alt=""
                    >

                    <div
                        class="playlist-track-name"
                        title="${escapePlaylistText(snippet.title)}"
                    >
                        ${escapePlaylistText(snippet.title)}
                    </div>

                `;


                /*
                 * 곡을 클릭하면 해당 영상으로 이동
                 */
                if (videoId) {

                    track.addEventListener(
                        "click",
                        function() {

                            playTravelPlaylistVideo(
                                videoId
                            );

                        }
                    );
                }


                trackList.appendChild(track);
            }
        );

    } catch (error) {

        console.error(
            "플레이리스트 곡 목록 오류:",
            error
        );

        trackList.innerHTML = `
            <div class="playlist-empty">
                곡 목록을 불러오지 못했습니다.
            </div>
        `;

        if (trackCount) {
            trackCount.innerText = "0곡";
        }
    }
}


/* =========================================================
   곡 클릭해서 재생
   ========================================================= */

function playTravelPlaylistVideo(
    videoId
) {

    const iframe =
        document.getElementById(
            "youtube-playlist-player"
        );

    if (!iframe || !videoId) return;


    iframe.src =
        "https://www.youtube.com/embed/" +
        encodeURIComponent(videoId) +
        "?autoplay=1&rel=0";
}


/* =========================================================
   YouTube 플레이리스트 열기
   ========================================================= */

function openCurrentYouTubePlaylist() {

    if (!currentTravelPlaylistUrl) {

        alert(
            "아직 오늘의 플레이리스트를 불러오지 않았습니다."
        );

        return;
    }


    window.open(
        currentTravelPlaylistUrl,
        "_blank"
    );
}


/* =========================================================
   HTML 특수문자 처리
   ========================================================= */

function escapePlaylistText(text) {

    if (!text) return "";

    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


/* =========================================================
   페이지 로딩 완료
   ========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    function() {

        const iframe =
            document.getElementById(
                "youtube-playlist-player"
            );

        if (iframe) {
            iframe.src = "";
        }

    }
);