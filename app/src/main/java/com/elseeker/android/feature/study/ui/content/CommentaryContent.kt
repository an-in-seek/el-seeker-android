package com.elseeker.android.feature.study.ui.content

/** 성경 주석 사이트 큐레이션 — 원본: static/js/study/bible-commentary.js, templates/study/bible-commentary.html. */
internal val commentaryContent: StudyContentItem = StudyContentItem(
    key = "commentary",
    title = "성경 주석",
    subtitle = "주석 사이트 큐레이션",
    track = StudyTrack.S1,
    ready = true,
    paragraphs = listOf(
        "국내외에서 신뢰받는 성경 주석·연구 사이트를 큐레이션했습니다. 검색으로 빠르게 비교하세요.",
        "본 페이지에 소개된 외부 사이트의 콘텐츠와 신학적 견해는 각 사이트 운영 주체에 귀속됩니다. ElSeeker는 링크만 제공합니다.",
    ),
    linksTitle = "주석 사이트",
    links = listOf(
        StudyLink(
            title = "FreeBibleCommentary 한국어",
            subtitle = "Bob Utley 박사의 학술적 성경 주석을 한국어로 무료 제공 · 학술 주석, 한국어 번역, 무료",
            url = "https://www.freebiblecommentary.org/korean_bible_study.htm",
        ),
        StudyLink(
            title = "GotQuestions 한국어",
            subtitle = "절별 학술 주석보다는 신학 Q&A 형식. 성경 관련 8,000+ 질문을 주제·구절별로 검색하여 한국어 해설을 읽을 수 있다 · Q&A 형식, 주제별 해설, 신학 질문",
            url = "https://www.gotquestions.org/Korean/",
        ),
        StudyLink(
            title = "Bible Hub Commentaries (영어)",
            subtitle = "다양한 영어 주석(매튜 헨리·반즈 등)을 절별로 통합 제공 · 주석 모음, 절별 비교",
            url = "https://biblehub.com/commentaries",
        ),
        StudyLink(
            title = "Blue Letter Bible Commentaries (영어)",
            subtitle = "스트롱 코드·원어 분석과 함께 주석을 절별로 제공 (창세기 1:1 진입) · 스트롱, 원어, 주석",
            url = "https://www.blueletterbible.org/niv/gen/1/1/t_comms_1001",
        ),
        StudyLink(
            title = "Bible Gateway (영어)",
            subtitle = "200+ 번역본 비교, 오디오 성경, 묵상 도구를 통합 제공 · 다중 번역, 오디오, 묵상",
            url = "https://www.biblegateway.com/",
        ),
        StudyLink(
            title = "StudyLight (영어)",
            subtitle = "고전 주석(매튜 헨리·John Gill 등)과 사전을 무료로 제공 · 고전 주석, 사전",
            url = "https://www.studylight.org/",
        ),
        StudyLink(
            title = "Internet Sacred Text Archive — Bible Commentaries (영어)",
            subtitle = "여러 고전 성경 주석을 한 곳에서 무료로 열람할 수 있는 디지털 아카이브 · 고전 주석, 디지털 아카이브, 무료",
            url = "https://sacred-texts.com/bib/cmt/index.htm",
        ),
        StudyLink(
            title = "CCEL — Calvin's Commentaries (영어)",
            subtitle = "장 칼빈의 성경 주석 전집을 무료로 열람할 수 있는 CCEL 디지털 아카이브 · 칼빈, 고전 주석, 종교개혁",
            url = "https://ccel.org/c/calvin/comment2/home.html",
        ),
        StudyLink(
            title = "NET Bible (Bible.org) (영어)",
            subtitle = "각주 6만 개 이상이 본문에 직접 연결된 학술적 영어 번역·주석 · 학술 각주, 번역 노트",
            url = "https://netbible.org/",
        ),
    ),
)
