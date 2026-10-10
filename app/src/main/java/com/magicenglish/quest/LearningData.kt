package com.magicenglish.quest

data class Word(
    val id: String,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val example: String
)

data class GrammarPoint(
    val id: String,
    val title: String,
    val tag: String,
    val examples: List<Pair<String, String>>,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val formula: String,
    val mnemonic: String,
    val practice: String,
    val practiceOptions: List<String>,
    val practiceAnswer: Int,
    val explanation: String
)

enum class Volume(val label: String, val shortLabel: String) {
    FIRST("上册", "上"),
    SECOND("下册", "下")
}

data class CourseBook(
    val id: String,
    val series: String,
    val grade: Int,
    val volume: Volume,
    val units: List<CourseUnit>
) {
    val displayName: String get() = "$grade 年级${volume.label}"
    val shortName: String get() = "$grade${volume.shortLabel}"
}

data class CourseUnit(
    val id: String,
    val number: Int,
    val title: String,
    val topic: String,
    val words: List<Word>,
    val grammar: List<GrammarPoint>
)

private data class UnitSeed(
    val title: String,
    val topic: String,
    val vocabulary: List<Pair<String, String>>,
    val pattern: String,
    val practice: String,
    val practiceOptions: List<String>,
    val practiceAnswer: Int
)

object LearningData {
    const val DEFAULT_BOOK_ID = "pep_g5_first"

    private val contextualExamples = mapOf(
        "pep_g5_first_u2" to mapOf(
            "monday" to "We have Chinese and English on Monday.",
            "tuesday" to "Our maths class is on Tuesday.",
            "wednesday" to "We draw pictures in art class on Wednesday.",
            "thursday" to "We have PE on Thursday afternoon.",
            "friday" to "The music class is on Friday.",
            "saturday" to "I help my parents at home on Saturday.",
            "sunday" to "We visit our grandparents on Sunday.",
            "weekend" to "I often read books on the weekend.",
            "wash" to "Please wash your hands before dinner.",
            "wash my clothes" to "I wash my clothes on Saturday morning.",
            "watch" to "We watch the birds in the park.",
            "watch tv" to "My family and I watch TV after dinner.",
            "do" to "I do my homework after school.",
            "do homework" to "I usually do homework in the evening.",
            "read" to "Please read this story aloud.",
            "read books" to "I like to read books in the library.",
            "play" to "The children play together after school.",
            "play football" to "We play football on the playground.",
            "cooking" to "I have a cooking class on Saturday.",
            "often" to "I often help my mother at home.",
            "park" to "We fly kites in the park.",
            "tired" to "I feel tired after playing football.",
            "sport" to "Basketball is my favourite sport.",
            "play sports" to "We play sports every afternoon.",
            "should" to "You should finish your homework first.",
            "every day" to "I practise English every day.",
            "schedule" to "I write my classes on the weekly schedule."
        )
    )

    private val supplementalVocabulary = mapOf(
        "pep_g3_first_u1" to listOf("pen" to "钢笔", "pencil box" to "铅笔盒", "book" to "书", "bag" to "书包", "no" to "不", "your" to "你的", "name" to "名字", "goodbye" to "再见"),
        "pep_g3_first_u2" to listOf("colour" to "颜色", "OK" to "好；行", "mum" to "妈妈", "show" to "给……看"),
        "pep_g3_first_u3" to listOf("body" to "身体", "leg" to "腿", "foot" to "脚", "school" to "学校"),
        "pep_g3_first_u4" to listOf("zoo" to "动物园", "bird" to "鸟", "panda" to "大熊猫", "funny" to "滑稽的"),
        "pep_g3_first_u5" to listOf("have" to "吃；喝", "eat" to "吃", "drink" to "喝", "please" to "请"),
        "pep_g3_first_u6" to listOf("eight" to "八", "nine" to "九", "brother" to "兄弟", "plate" to "盘子", "happy" to "快乐的", "birthday" to "生日"),
        "pep_g3_second_u1" to listOf("today" to "今天", "new" to "新的", "friend" to "朋友", "she" to "她", "he" to "他"),
        "pep_g3_second_u2" to listOf("grandmother" to "祖母；外祖母", "grandma" to "奶奶；外婆", "grandfather" to "祖父；外祖父", "grandpa" to "爷爷；外公"),
        "pep_g3_second_u3" to listOf("so" to "这么；那么", "children" to "儿童", "tail" to "尾巴"),
        "pep_g3_second_u4" to listOf("boat" to "小船", "map" to "地图", "toy" to "玩具", "box" to "盒子"),
        "pep_g3_second_u5" to listOf("buy" to "买", "like" to "喜欢"),
        "pep_g3_second_u6" to listOf("seventeen" to "十七", "nineteen" to "十九", "kite" to "风筝", "beautiful" to "美丽的"),

        "pep_g4_first_u1" to listOf("wall" to "墙壁", "floor" to "地板", "fan" to "风扇", "clean" to "打扫", "help" to "帮助"),
        "pep_g4_first_u2" to listOf("toy" to "玩具", "wow" to "哇", "so much" to "非常地", "lost" to "丢失的", "cute" to "可爱的"),
        "pep_g4_first_u3" to listOf("his" to "他的", "or" to "或者", "right" to "正确的"),
        "pep_g4_first_u4" to listOf("fridge" to "冰箱", "table" to "桌子", "find" to "找到", "them" to "他们；她们；它们"),
        "pep_g4_first_u5" to listOf("fork" to "餐叉", "knife" to "刀", "pass" to "给；递", "try" to "试；尝试"),
        "pep_g4_first_u6" to listOf("nurse" to "护士", "farmer" to "农民", "football player" to "足球运动员", "job" to "工作"),
        "pep_g4_second_u1" to listOf("homework" to "作业", "class" to "班；班级", "forty" to "四十", "way" to "方向"),
        "pep_g4_second_u2" to listOf("over" to "结束", "now" to "现在", "kid" to "小孩", "thirty" to "三十", "hurry" to "匆忙", "come on" to "快；加油"),
        "pep_g4_second_u3" to listOf("rainy" to "下雨的", "outside" to "在户外", "be careful" to "小心", "weather" to "天气", "degree" to "度数", "world" to "世界"),
        "pep_g4_second_u4" to listOf("these" to "这些", "those" to "那些", "animal" to "动物", "garden" to "花园", "goat" to "山羊"),
        "pep_g4_second_u5" to listOf("whose" to "谁的", "mine" to "我的", "pack" to "收拾", "wait" to "等待"),
        "pep_g4_second_u6" to listOf("size" to "尺码", "try on" to "试穿", "of course" to "当然", "too" to "太；也", "eighty" to "八十"),

        "pep_g5_first_u1" to listOf(
            "be good at" to "擅长", "work together" to "一起合作", "football team" to "足球队",
            "jump rope" to "跳绳", "speak Chinese" to "说汉语", "play the piano" to "弹钢琴",
            "play Chinese chess" to "下中国象棋", "play the pipa" to "弹琵琶",
            "would like to" to "想要", "help a lot" to "帮了很多忙", "wait for" to "等待",
            "in front of" to "在……前面", "Science and Art Festival" to "科技艺术节",
            "football match" to "足球比赛", "come to life" to "活起来", "what a pity" to "太遗憾了"
        ),
        "pep_g5_first_u2" to listOf(
            "look sad" to "看起来难过", "be angry with" to "生……的气", "what's the matter" to "怎么回事",
            "talk to your parents" to "和父母谈谈", "school project" to "学校项目",
            "ask your teacher for help" to "向老师求助", "children's centre" to "儿童中心",
            "listen to your friends" to "倾听朋友", "clean up the park" to "清理公园",
            "agree with each other" to "彼此达成一致", "share ideas" to "分享想法",
            "finish our project" to "完成我们的项目", "look for" to "寻找", "by Monday" to "到周一之前",
            "make someone happy" to "使某人开心", "use kind words" to "使用友善的话语",
            "maths test" to "数学测验", "feel better" to "感觉好些", "count to ten" to "数到十",
            "try new things" to "尝试新事物"
        ),
        "pep_g5_first_u3" to listOf(
            "have PE" to "上体育课", "go running" to "去跑步", "favourite subject" to "最喜欢的科目",
            "weekend activities" to "周末活动", "have pipa lessons" to "上琵琶课",
            "look after" to "照顾", "tidy my room" to "整理房间", "do many chores" to "做许多家务",
            "make a schedule" to "制定计划表", "play ping-pong" to "打乒乓球",
            "visit grandparents" to "看望祖父母", "do homework on time" to "按时完成作业",
            "take breaks" to "休息一下", "stay healthy" to "保持健康", "eat well" to "健康饮食",
            "get enough sleep" to "保证充足睡眠", "to-do list" to "待办清单",
            "balance work and play" to "平衡学习与娱乐"
        ),
        "pep_g5_first_u4" to listOf(
            "stay healthy" to "保持健康", "have a cold" to "感冒", "wash hands" to "洗手",
            "exercise every day" to "每天锻炼", "drink enough water" to "喝足够的水",
            "eat less candy" to "少吃糖果", "eat breakfast" to "吃早饭",
            "rest your eyes" to "让眼睛休息", "eat healthy food" to "吃健康食物",
            "brush your teeth" to "刷牙", "get enough sleep" to "保证充足睡眠",
            "take care of" to "爱护；照顾"
        ),
        "pep_g5_first_u5" to listOf(
            "would like" to "想要", "beef noodles" to "牛肉面", "a little" to "一点儿",
            "chicken soup" to "鸡汤", "green tea" to "绿茶", "come from" to "来自",
            "plant seeds" to "播种", "grow from seeds" to "由种子长成",
            "cook meals" to "做饭"
        ),
        "pep_g5_first_u6" to listOf(
            "nature park" to "自然公园", "go hiking" to "去徒步旅行", "think of" to "想；思考",
            "draw pictures" to "画画", "close the door" to "关门", "heavy rain" to "大雨",
            "heavy snow" to "大雪", "bad weather" to "恶劣天气", "be careful of" to "小心",
            "check the weather" to "查看天气", "wear good shoes" to "穿合适的鞋",
            "stay in the hotel" to "待在旅馆", "bring enough water" to "带足够的水",
            "go on a trip" to "去旅行"
        ),
        "pep_g5_second_u1" to listOf("take a dancing class" to "上舞蹈课", "go for a walk" to "散步", "usually" to "通常", "often" to "经常", "sometimes" to "有时", "when" to "什么时候", "after" to "在……之后", "start" to "开始"),
        "pep_g5_second_u2" to listOf("go on a picnic" to "去野餐", "go swimming" to "去游泳", "which" to "哪一个", "best" to "最；最高程度", "snow" to "雪", "because" to "因为", "vacation" to "假期"),
        "pep_g5_second_u3" to listOf("September" to "九月", "October" to "十月", "November" to "十一月", "December" to "十二月", "contest" to "比赛", "trip" to "旅行", "year" to "年"),
        "pep_g5_second_u4" to listOf("twenty-first" to "第二十一", "twenty-third" to "第二十三", "thirtieth" to "第三十", "special" to "特别的", "kitten" to "小猫", "diary" to "日记"),
        "pep_g5_second_u5" to listOf("sleeping" to "正在睡觉", "drinking" to "正在喝水", "jumping" to "正在跳", "running" to "正在跑", "walking" to "正在走", "looking" to "正在看", "excited" to "兴奋的"),
        "pep_g5_second_u6" to listOf("show" to "给人看", "anything" to "任何事物", "else" to "另外；其他", "exhibition" to "展览", "say" to "说", "work" to "工作"),

        "pep_g6_first_u1" to listOf("left" to "左边", "right" to "右边", "straight" to "笔直地", "ask" to "问", "interesting" to "有趣的", "Italian" to "意大利的"),
        "pep_g6_first_u2" to listOf("train" to "火车", "traffic lights" to "交通信号灯", "pay attention to" to "注意", "Munich" to "慕尼黑", "Germany" to "德国", "Alaska" to "阿拉斯加"),
        "pep_g6_first_u3" to listOf("comic book" to "漫画书", "word book" to "单词书", "lesson" to "课", "space" to "太空", "travel" to "旅行", "half" to "一半", "price" to "价格"),
        "pep_g6_first_u4" to listOf("dancing" to "跳舞", "singing" to "唱歌", "reading stories" to "读故事", "playing football" to "踢足球", "likes" to "喜欢", "goal" to "目标", "join" to "加入"),
        "pep_g6_first_u5" to listOf("head teacher" to "校长", "reporter" to "记者", "secretary" to "秘书", "university" to "大学", "gym" to "体育馆", "if" to "如果"),
        "pep_g6_first_u6" to listOf("ill" to "有病的", "wrong" to "有问题的", "should" to "应该", "feel" to "觉得", "well" to "健康；好", "count" to "数数"),
        "pep_g6_second_u1" to listOf("dinosaur" to "恐龙", "hall" to "大厅", "metre" to "米", "both" to "两个都", "kilogram" to "千克", "size" to "尺码"),
        "pep_g6_second_u2" to listOf("drank" to "喝", "show" to "演出", "yesterday" to "昨天", "before" to "以前", "better" to "更好的", "last" to "最近的"),
        "pep_g6_second_u3" to listOf("fell off" to "从……摔下来", "mule" to "骡子", "Turpan" to "吐鲁番", "could" to "能", "till" to "直到", "beach" to "海滩"),
        "pep_g6_second_u4" to listOf("star" to "星", "easy" to "容易的", "Internet" to "互联网", "dream" to "梦", "race" to "赛跑", "nothing" to "没有什么", "active" to "积极的")
    )

    val books: List<CourseBook> = listOf(
        book(3, Volume.FIRST, listOf(
            seed("Hello!", "问候与文具", "hello" to "你好", "hi" to "嗨", "I" to "我", "am" to "是", "ruler" to "尺子", "pencil" to "铅笔", "eraser" to "橡皮", "crayon" to "蜡笔", pattern = "Hello, I'm ...", practice = "Hello, ___ Wu Binbin.", options = listOf("I'm", "is", "are"), answer = 0),
            seed("Colours", "颜色", "red" to "红色", "green" to "绿色", "yellow" to "黄色", "blue" to "蓝色", "black" to "黑色", "brown" to "棕色", "white" to "白色", "orange" to "橙色", pattern = "What colour is it? It's ...", practice = "What colour is it? ___ red.", options = listOf("It's", "I'm", "They're"), answer = 0),
            seed("Look at me!", "身体部位", "face" to "脸", "ear" to "耳朵", "eye" to "眼睛", "nose" to "鼻子", "mouth" to "嘴", "arm" to "胳膊", "hand" to "手", "head" to "头", pattern = "Look at me. This is my ...", practice = "This ___ my nose.", options = listOf("is", "are", "am"), answer = 0),
            seed("We love animals", "动物", "duck" to "鸭子", "pig" to "猪", "cat" to "猫", "bear" to "熊", "dog" to "狗", "elephant" to "大象", "monkey" to "猴子", "tiger" to "老虎", pattern = "What's this? It's a ...", practice = "What's this? It's ___ elephant.", options = listOf("an", "a", "the"), answer = 0),
            seed("Let's eat!", "食物与饮料", "bread" to "面包", "juice" to "果汁", "egg" to "鸡蛋", "milk" to "牛奶", "water" to "水", "cake" to "蛋糕", "fish" to "鱼肉", "rice" to "米饭", pattern = "I'd like some ..., please.", practice = "I'd like ___ water, please.", options = listOf("some", "a", "an"), answer = 0),
            seed("Happy birthday!", "年龄与数字", "one" to "一", "two" to "二", "three" to "三", "four" to "四", "five" to "五", "six" to "六", "seven" to "七", "ten" to "十", pattern = "How old are you? I'm ... years old.", practice = "How old ___ you?", options = listOf("are", "is", "am"), answer = 0)
        )),
        book(3, Volume.SECOND, listOf(
            seed("Welcome back to school", "国家与同学", "UK" to "英国", "Canada" to "加拿大", "USA" to "美国", "China" to "中国", "student" to "学生", "pupil" to "小学生", "teacher" to "教师", "boy" to "男孩", pattern = "Where are you from? I'm from ...", practice = "I ___ from China.", options = listOf("am", "is", "are"), answer = 0),
            seed("My family", "家庭成员", "father" to "父亲", "dad" to "爸爸", "man" to "男人", "woman" to "女人", "mother" to "母亲", "sister" to "姐姐；妹妹", "brother" to "哥哥；弟弟", "family" to "家庭", pattern = "Who's that ...? He/She is my ...", practice = "Who's that woman? ___ is my mother.", options = listOf("She", "He", "It"), answer = 0),
            seed("At the zoo", "动物特征", "thin" to "瘦的", "fat" to "胖的", "tall" to "高的", "short" to "矮的", "long" to "长的", "small" to "小的", "big" to "大的", "giraffe" to "长颈鹿", pattern = "It is tall and thin.", practice = "The elephant ___ a long nose.", options = listOf("has", "have", "is"), answer = 0),
            seed("Where is my car?", "位置与玩具", "on" to "在……上", "in" to "在……里", "under" to "在……下面", "chair" to "椅子", "desk" to "书桌", "cap" to "帽子", "ball" to "球", "car" to "小汽车", pattern = "Where is ...? It's in/on/under ...", practice = "Where is the ball? It's ___ the desk.", options = listOf("under", "have", "from"), answer = 0),
            seed("Do you like pears?", "水果与喜好", "pear" to "梨", "apple" to "苹果", "orange" to "橙子", "banana" to "香蕉", "watermelon" to "西瓜", "strawberry" to "草莓", "grape" to "葡萄", "fruit" to "水果", pattern = "Do you like ...? Yes, I do.", practice = "Do you like apples? Yes, I ___.", options = listOf("do", "am", "like"), answer = 0),
            seed("How many?", "数量", "eleven" to "十一", "twelve" to "十二", "thirteen" to "十三", "fourteen" to "十四", "fifteen" to "十五", "sixteen" to "十六", "eighteen" to "十八", "twenty" to "二十", pattern = "How many ... do you see?", practice = "How many kites ___ you see?", options = listOf("do", "are", "is"), answer = 0)
        )),
        book(4, Volume.FIRST, listOf(
            seed("My classroom", "教室", "classroom" to "教室", "window" to "窗户", "blackboard" to "黑板", "light" to "电灯", "picture" to "图画", "door" to "门", "teacher's desk" to "讲台", "computer" to "计算机", pattern = "Where is it? It's near ...", practice = "The picture is ___ the window.", options = listOf("near", "many", "friendly"), answer = 0),
            seed("My schoolbag", "书包与学习用品", "schoolbag" to "书包", "maths book" to "数学书", "English book" to "英语书", "Chinese book" to "语文书", "storybook" to "故事书", "candy" to "糖果", "notebook" to "笔记本", "key" to "钥匙", pattern = "What's in your schoolbag?", practice = "What ___ in your schoolbag?", options = listOf("is", "are", "am"), answer = 0),
            seed("My friends", "朋友外貌与性格", "strong" to "强壮的", "friendly" to "友好的", "quiet" to "安静的", "hair" to "头发", "shoe" to "鞋", "glasses" to "眼镜", "hat" to "帽子", "friend" to "朋友", pattern = "He/She is ... and has ...", practice = "She ___ long hair.", options = listOf("has", "have", "is"), answer = 0),
            seed("My home", "房间与家具", "bedroom" to "卧室", "living room" to "客厅", "study" to "书房", "kitchen" to "厨房", "bathroom" to "浴室", "bed" to "床", "phone" to "电话", "sofa" to "沙发", pattern = "Is she in the ...? Yes, she is.", practice = "Is he in the study? Yes, he ___.", options = listOf("is", "does", "has"), answer = 0),
            seed("Dinner's ready", "餐具与用餐", "beef" to "牛肉", "chicken" to "鸡肉", "noodles" to "面条", "soup" to "汤", "vegetable" to "蔬菜", "chopsticks" to "筷子", "bowl" to "碗", "spoon" to "勺", pattern = "What would you like? I'd like ...", practice = "What would you ___?", options = listOf("like", "likes", "liked"), answer = 0),
            seed("Meet my family!", "家庭与职业", "parents" to "父母", "cousin" to "堂表兄弟姐妹", "uncle" to "叔伯舅", "aunt" to "姑姨", "baby brother" to "婴儿弟弟", "doctor" to "医生", "cook" to "厨师", "driver" to "司机", pattern = "What's his/her job? He/She is a ...", practice = "What's her job? She ___ a doctor.", options = listOf("is", "are", "does"), answer = 0)
        )),
        book(4, Volume.SECOND, listOf(
            seed("My school", "学校场所", "first floor" to "一楼", "second floor" to "二楼", "teachers' office" to "教师办公室", "library" to "图书馆", "playground" to "操场", "computer room" to "计算机房", "art room" to "美术教室", "music room" to "音乐教室", pattern = "Where is the ...? It's on the ... floor.", practice = "The library is ___ the first floor.", options = listOf("on", "in", "at"), answer = 0),
            seed("What time is it?", "时间与日程", "breakfast" to "早餐", "English class" to "英语课", "lunch" to "午餐", "music class" to "音乐课", "PE class" to "体育课", "dinner" to "晚餐", "get up" to "起床", "go to school" to "去上学", pattern = "What time is it? It's ... o'clock.", practice = "What time ___ it?", options = listOf("is", "are", "does"), answer = 0),
            seed("Weather", "天气", "cold" to "寒冷的", "cool" to "凉爽的", "warm" to "温暖的", "hot" to "炎热的", "sunny" to "晴朗的", "windy" to "多风的", "cloudy" to "多云的", "snowy" to "下雪的", pattern = "What's the weather like? It's ...", practice = "___ the weather like today?", options = listOf("What's", "How", "Where"), answer = 0),
            seed("At the farm", "农场动物与蔬菜", "tomato" to "西红柿", "potato" to "土豆", "green beans" to "豆角", "carrot" to "胡萝卜", "horse" to "马", "cow" to "奶牛", "sheep" to "绵羊", "hen" to "母鸡", pattern = "Are these ...? Yes, they are.", practice = "Are these carrots? Yes, they ___.", options = listOf("are", "is", "do"), answer = 0),
            seed("My clothes", "服装", "clothes" to "衣服", "pants" to "裤子", "hat" to "帽子", "dress" to "连衣裙", "skirt" to "女裙", "coat" to "外衣", "sweater" to "毛衣", "sock" to "短袜", pattern = "Whose ... is this? It's mine.", practice = "Whose coat is this? It's ___.", options = listOf("mine", "my", "me"), answer = 0),
            seed("Shopping", "购物与价格", "glove" to "手套", "scarf" to "围巾", "umbrella" to "雨伞", "sunglasses" to "太阳镜", "pretty" to "美观的", "expensive" to "昂贵的", "cheap" to "便宜的", "dollar" to "美元", pattern = "How much is it? It's ... yuan.", practice = "How much ___ the scarf?", options = listOf("is", "are", "do"), answer = 0)
        )),
        book(5, Volume.FIRST, listOf(
            seed("Different friends", "朋友的特点与能力",
                "team" to "队；组", "player" to "运动员；参赛选手", "him" to "他（宾格）",
                "lovely" to "可爱的；美丽的", "clever" to "聪明的", "young" to "年轻的",
                "hard-working" to "勤奋的", "science" to "科学", "robot" to "机器人",
                "jump" to "跳；跃", "rope" to "绳", "piano" to "钢琴", "chess" to "国际象棋",
                "front" to "前面；前面的", "easy" to "容易的", "star" to "明星",
                "wonderful" to "精彩的", "Australia" to "澳大利亚",
                pattern = "How are your friends different? What's he/she like? He/She is ...",
                practice = "What's your friend ___?", options = listOf("like", "likes", "look"), answer = 0),
            seed("My feelings", "情绪表达与沟通",
                "sad" to "难过的", "matter" to "事情；问题", "project" to "项目；方案",
                "centre" to "中心", "agree" to "同意", "finish" to "完成", "Monday" to "星期一",
                "worried" to "担心的", "both" to "两者；两者的", "excited" to "兴奋的",
                "wrong" to "错误的", "angry" to "生气的", "afraid" to "害怕的",
                "parent" to "父亲或母亲", "ask" to "询问", "move" to "移动；搬家", "yours" to "你的",
                pattern = "You look sad. What's the matter? I feel ...",
                practice = "What's ___ matter?", options = listOf("the", "a", "an"), answer = 0),
            seed("Work and play", "课程、周末与劳逸结合",
                "Wednesday" to "星期三", "subject" to "科目", "Monday" to "星期一",
                "Tuesday" to "星期二", "Friday" to "星期五", "because" to "因为",
                "Thursday" to "星期四", "start" to "开始", "usually" to "通常",
                "weekend" to "周末", "sometimes" to "有时", "Saturday" to "星期六",
                "Sunday" to "星期日", "lesson" to "课", "evening" to "傍晚；晚上",
                "answer" to "回答", "ping-pong" to "乒乓球", "night" to "夜晚",
                "visit" to "看望；参观", "grandparent" to "祖父母；外祖父母",
                pattern = "What do you have on Wednesdays? I have Chinese, maths, music and PE.",
                practice = "What do you have ___ Wednesdays?", options = listOf("on", "in", "at"), answer = 0),
            seed("Healthy habits", "健康习惯与建议",
                "stay" to "保持；待", "should" to "应该", "flu" to "流感", "else" to "其他的",
                "exercise" to "锻炼", "enough" to "足够的", "if" to "如果", "habit" to "习惯",
                "show" to "节目；展示", "phone" to "电话", "then" to "那么；然后",
                "rest" to "休息", "take care of" to "爱护；照顾", "check" to "查看",
                "less" to "较少的", "brush" to "刷", "tooth" to "牙齿", "twice" to "两次",
                "hour" to "小时", "free" to "空闲的",
                pattern = "How can we stay healthy? We should ...",
                practice = "We should ___ enough water.", options = listOf("drink", "drinks", "drinking"), answer = 0),
            seed("Food we eat", "食物、点餐与植物来源",
                "hungry" to "饿的", "beef" to "牛肉", "ice cream" to "冰淇淋",
                "little" to "少许；一点", "dumpling" to "饺子；汤圆", "tea" to "茶",
                "hamburger" to "汉堡包", "Mrs" to "夫人", "coconut" to "椰子",
                "interesting" to "有趣的", "seed" to "种子", "pull" to "拔；拉",
                "pull up" to "拔出", "stem" to "茎；梗", "root" to "根", "lotus" to "莲",
                "lake" to "湖", "world" to "世界", "round" to "圆形的",
                "different" to "不同的", "way" to "方法；方式",
                pattern = "What would you like to eat? I'd like ...",
                practice = "I'd like ___ beef and rice.", options = listOf("some", "a", "an"), answer = 0),
            seed("Nature and us", "自然景观、天气与安全",
                "river" to "河流", "nature" to "自然界", "waterfall" to "瀑布",
                "forest" to "森林", "famous" to "著名的", "mountain" to "高山",
                "wind" to "风", "trip" to "旅行", "dangerous" to "危险的",
                "heavy" to "严重的；程度大的", "hotel" to "旅馆", "close" to "关闭",
                "umbrella" to "雨伞", "raincoat" to "雨衣", "worry" to "担心",
                "fine" to "晴朗的", "fog" to "雾", "flood" to "洪水",
                "fire" to "火；火灾", "know" to "知道",
                pattern = "Is there a waterfall in the forest? Yes, there is.",
                practice = "There ___ a big forest in the park.", options = listOf("is", "are", "have"), answer = 0)
        )),
        book(5, Volume.SECOND, listOf(
            seed("My day", "日常作息", "eat breakfast" to "吃早饭", "have class" to "上课", "play sports" to "做运动", "exercise" to "活动；运动", "do morning exercises" to "做早操", "eat dinner" to "吃晚饭", "clean my room" to "打扫房间", "go shopping" to "购物", pattern = "When do you ...? I usually ... at ...", practice = "When do you ___ breakfast?", options = listOf("eat", "eats", "eating"), answer = 0),
            seed("My favourite season", "季节与活动", "spring" to "春天", "summer" to "夏天", "autumn" to "秋天", "winter" to "冬天", "season" to "季节", "picnic" to "野餐", "pick apples" to "摘苹果", "make a snowman" to "堆雪人", pattern = "Which season do you like best?", practice = "Which season do you like ___?", options = listOf("best", "good", "better"), answer = 0),
            seed("My school calendar", "月份与节日", "January" to "一月", "February" to "二月", "March" to "三月", "April" to "四月", "May" to "五月", "June" to "六月", "July" to "七月", "August" to "八月", pattern = "When is the ...? It's in ...", practice = "When is the trip? It's ___ May.", options = listOf("in", "on", "at"), answer = 0),
            seed("When is the art show?", "日期", "first" to "第一", "second" to "第二", "third" to "第三", "fourth" to "第四", "fifth" to "第五", "twelfth" to "第十二", "twentieth" to "第二十", "birthday" to "生日", pattern = "When is ...? It's on ...", practice = "My birthday is ___ April 4th.", options = listOf("on", "in", "at"), answer = 0),
            seed("Whose dog is it?", "物主与动作", "mine" to "我的", "yours" to "你们的", "his" to "他的", "hers" to "她的", "theirs" to "他们的", "climbing" to "攀爬", "eating" to "吃", "playing" to "玩耍", pattern = "Whose ... is it? It's mine/yours.", practice = "The yellow picture is ___.", options = listOf("mine", "my", "me"), answer = 0),
            seed("Work quietly!", "行为规范", "doing morning exercises" to "正在做早操", "having class" to "正在上课", "eating lunch" to "正在吃午饭", "reading a book" to "正在读书", "listening to music" to "正在听音乐", "keep" to "保持", "quietly" to "安静地", "take turns" to "按顺序来", pattern = "What are they doing? They are ...", practice = "They are ___ a book.", options = listOf("reading", "read", "reads"), answer = 0)
        )),
        book(6, Volume.FIRST, listOf(
            seed("How can I get there?", "地点与问路", "science museum" to "科学博物馆", "post office" to "邮局", "bookstore" to "书店", "cinema" to "电影院", "hospital" to "医院", "crossing" to "十字路口", "turn left" to "左转", "turn right" to "右转", pattern = "Where is ...? How can I get there?", practice = "Turn right ___ the hospital.", options = listOf("at", "in", "on"), answer = 0),
            seed("Ways to go to school", "交通方式", "on foot" to "步行", "by bus" to "乘公共汽车", "by plane" to "乘飞机", "by taxi" to "乘出租车", "by ship" to "乘船", "by subway" to "乘地铁", "slow down" to "慢下来", "stop" to "停下", pattern = "How do you come to school? By ...", practice = "How ___ you come to school?", options = listOf("do", "does", "are"), answer = 0),
            seed("My weekend plan", "周末计划", "visit my grandparents" to "看望祖父母", "see a film" to "看电影", "take a trip" to "去旅行", "go to the supermarket" to "去超市", "tonight" to "今晚", "tomorrow" to "明天", "dictionary" to "词典", "postcard" to "明信片", pattern = "What are you going to do?", practice = "I'm going to ___ a film.", options = listOf("see", "saw", "seeing"), answer = 0),
            seed("I have a pen pal", "爱好与日常", "studies Chinese" to "学习中文", "does word puzzles" to "猜字谜", "goes hiking" to "去远足", "cooks Chinese food" to "做中餐", "hobby" to "爱好", "pen pal" to "笔友", "club" to "俱乐部", "shall" to "表示征求意见", pattern = "What are his hobbies? He likes ...", practice = "He likes ___ football.", options = listOf("playing", "play", "plays"), answer = 0),
            seed("What does he do?", "职业", "factory worker" to "工厂工人", "postman" to "邮递员", "businessman" to "商人", "police officer" to "警察", "fisherman" to "渔民", "scientist" to "科学家", "pilot" to "飞行员", "coach" to "教练", pattern = "What does he/she do? He/She is a ...", practice = "What ___ your father do?", options = listOf("does", "do", "is"), answer = 0),
            seed("How do you feel?", "情绪与建议", "angry" to "生气的", "afraid" to "害怕的", "sad" to "难过的", "worried" to "担心的", "happy" to "高兴的", "see a doctor" to "看医生", "take a deep breath" to "深呼吸", "wear warm clothes" to "穿暖和衣服", pattern = "How do you feel? I feel ...", practice = "You should ___ a deep breath.", options = listOf("take", "takes", "taking"), answer = 0)
        )),
        book(6, Volume.SECOND, listOf(
            seed("How tall are you?", "身高体重与比较", "younger" to "更年轻的", "older" to "更年长的", "taller" to "更高的", "shorter" to "更矮的", "longer" to "更长的", "thinner" to "更瘦的", "heavier" to "更重的", "stronger" to "更强壮的", pattern = "How tall are you? I'm ... metres.", practice = "You are ___ than me.", options = listOf("taller", "tall", "tallest"), answer = 0),
            seed("Last weekend", "周末活动与过去式", "cleaned my room" to "打扫了房间", "washed my clothes" to "洗了衣服", "stayed at home" to "待在家", "watched TV" to "看了电视", "read a book" to "读了书", "saw a film" to "看了电影", "had a cold" to "感冒了", "slept" to "睡觉了", pattern = "What did you do last weekend?", practice = "I ___ TV last night.", options = listOf("watched", "watch", "watches"), answer = 0),
            seed("Where did you go?", "旅行与假期", "went camping" to "去野营了", "went fishing" to "去钓鱼了", "rode a horse" to "骑马了", "rode a bike" to "骑自行车了", "hurt my foot" to "脚受伤了", "ate fresh food" to "吃了新鲜食物", "took pictures" to "拍了照片", "bought gifts" to "买了礼物", pattern = "Where did you go? I went to ...", practice = "Where ___ you go?", options = listOf("did", "do", "were"), answer = 0),
            seed("Then and now", "今昔变化", "dining hall" to "饭厅", "grass" to "草坪", "gym" to "体育馆", "ago" to "以前", "cycling" to "骑自行车运动", "ice-skate" to "滑冰", "badminton" to "羽毛球运动", "different" to "不同的", pattern = "There was/were no ... before.", practice = "There ___ no gym in my school before.", options = listOf("was", "is", "are"), answer = 0)
        ))
    )

    val defaultBook: CourseBook get() = book(DEFAULT_BOOK_ID)
    val defaultUnit: CourseUnit get() = defaultBook.units.first()

    fun book(id: String): CourseBook = books.firstOrNull { it.id == id } ?: books.first()

    fun unit(bookId: String, unitId: String): CourseUnit {
        val selectedBook = book(bookId)
        return selectedBook.units.firstOrNull { it.id == unitId } ?: selectedBook.units.first()
    }

    private fun book(grade: Int, volume: Volume, seeds: List<UnitSeed>): CourseBook {
        val bookId = "pep_g${grade}_${volume.name.lowercase()}"
        return CourseBook(
            id = bookId,
            series = if (grade == 5 && volume == Volume.FIRST) "人教版 PEP（2026秋版）" else "人教版 PEP",
            grade = grade,
            volume = volume,
            units = seeds.mapIndexed { index, item -> createUnit(bookId, index + 1, item) }
        )
    }

    private fun createUnit(bookId: String, number: Int, seed: UnitSeed): CourseUnit {
        val unitId = "${bookId}_u$number"
        val fullVocabulary = (seed.vocabulary + supplementalVocabulary.orEmpty(unitId))
            .distinctBy { it.first.lowercase() }
        val words = fullVocabulary.mapIndexed { index, (term, meaning) ->
            Word(
                id = "${unitId}_w${index + 1}",
                word = term,
                phonetic = "点击喇叭听标准发音",
                meaning = meaning,
                example = exampleFor(unitId, term, index)
            )
        }
        val grammar = GrammarPoint(
            id = "${unitId}_g1",
            title = "核心句型",
            tag = seed.topic,
            examples = listOf(
                "课本情境" to seed.pattern,
                "表达提示" to "替换关键词，就能说出自己的新句子。"
            ),
            question = "下面哪一句是本单元的核心表达？",
            options = listOf(seed.pattern, "Nice to meet you.", "Have a good day."),
            answer = 0,
            formula = seed.pattern,
            mnemonic = "先认关键词，再看语序，最后大声完整表达。",
            practice = seed.practice,
            practiceOptions = seed.practiceOptions,
            practiceAnswer = seed.practiceAnswer,
            explanation = "回到核心句型，观察空格前后的词再选择。"
        )
        return CourseUnit(unitId, number, seed.title, seed.topic, words, listOf(grammar))
    }

    private fun exampleFor(unitId: String, term: String, index: Int): String {
        contextualExamples[unitId]?.get(term.lowercase())?.let { return it }
        return when (index % 4) {
            0 -> "Listen for the word \"$term\" in today's English story."
            1 -> "Our teacher uses \"$term\" in a complete sentence."
            2 -> "Can you use \"$term\" when you speak English?"
            else -> "Write a short sentence with the word \"$term\"."
        }
    }

    private fun seed(
        title: String,
        topic: String,
        vararg vocabulary: Pair<String, String>,
        pattern: String,
        practice: String,
        options: List<String>,
        answer: Int
    ) = UnitSeed(title, topic, vocabulary.toList(), pattern, practice, options, answer)

    private fun <K, V> Map<K, List<V>>.orEmpty(key: K): List<V> = this[key].orEmpty()
}
