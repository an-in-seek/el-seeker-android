package com.elseeker.android.feature.study.ui.content

/** 창조 (창세기 1–2장) — 원본: templates/study/creation.html. 텍스트는 원문 그대로. */
internal val creationContent: StudyContentItem = StudyContentItem(
    key = "creation",
    title = "창조",
    subtitle = "창세기 1–2장",
    track = StudyTrack.S1,
    ready = true,
    paragraphs = listOf(
        "태초에 하나님이 천지를 창조하시니라",
        "땅이 혼돈하고 공허하며",
        "흑암이 깊음 위에 있고",
        "하나님의 영은 수면 위에 운행하시니라",
    ),
    history = listOf(
        StudySection(
            "마치며",
            "하나님이 지으신 그 모든 것을 보시니 보시기에 심히 좋았더라",
        ),
    ),
    cardsTitle = "일곱 날의 창조",
    cards = listOf(
        StudyCard(
            tag = "첫째 날",
            title = "빛이 있으라",
            meaning = "하나님이 이르시되 빛이 있으라 하시니 빛이 있었고 그 빛이 하나님이 보시기에 좋았더라",
            quote = "저녁이 되고 아침이 되니 이는 첫째 날이니라",
        ),
        StudyCard(
            tag = "둘째 날",
            title = "물 가운데 궁창이 있어 물과 물로 나뉘라",
            meaning = "하나님이 궁창을 만드사 궁창 아래의 물과 궁창 위의 물로 나뉘게 하시니 그대로 되니라",
            quote = "저녁이 되고 아침이 되니 이는 둘째 날이니라",
        ),
        StudyCard(
            tag = "셋째 날",
            title = "천하의 물이 한 곳으로 모이고 뭍이 드러나라",
            meaning = "하나님이 뭍을 땅이라 부르시고 모인 물을 바다라 부르시니 하나님이 보시기에 좋았더라",
            quote = "저녁이 되고 아침이 되니 이는 셋째 날이니라",
        ),
        StudyCard(
            tag = "넷째 날",
            title = "하늘의 궁창에 광명체들이 있으라",
            meaning = "하나님이 두 큰 광명체를 만드사 큰 광명체로 낮을 주관하게 하시고 작은 광명체로 밤을 주관하게 하시며 또 별들을 만드시고",
            quote = "저녁이 되고 아침이 되니 이는 넷째 날이니라",
        ),
        StudyCard(
            tag = "다섯째 날",
            title = "물들은 생물을 번성하게 하라 새는 하늘의 궁창에서 날으라",
            meaning = "하나님이 큰 바다 짐승들과 물에서 번성하여 움직이는 모든 생물을 그 종류대로 창조하시니 하나님이 보시기에 좋았더라",
            quote = "저녁이 되고 아침이 되니 이는 다섯째 날이니라",
        ),
        StudyCard(
            tag = "여섯째 날",
            title = "우리의 형상을 따라 우리가 사람을 만들고",
            meaning = "하나님이 자기 형상 곧 하나님의 형상대로 사람을 창조하시되 남자와 여자를 창조하시고 하나님이 그들에게 복을 주시며 이르시되 생육하고 번성하여 땅에 충만하라",
            quote = "하나님이 지으신 그 모든 것을 보시니 보시기에 심히 좋았더라",
        ),
        StudyCard(
            tag = "일곱째 날",
            title = "하나님이 그 일곱째 날을 복되게 하사 거룩하게 하셨으니",
            meaning = "천지와 만물이 다 이루어지니라 하나님이 그가 하시던 일을 일곱째 날에 마치시니 그가 하시던 모든 일을 그치고 일곱째 날에 안식하시니라",
        ),
    ),
)
