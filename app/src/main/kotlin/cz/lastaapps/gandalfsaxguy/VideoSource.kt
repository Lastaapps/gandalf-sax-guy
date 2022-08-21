package cz.lastaapps.gandalfsaxguy

data class VideoSource(
    val id: Int,
    val resId: Int,
    val link: String,
) {
    companion object {
        val sources = listOf(
            VideoSource(
                0,
                R.raw.gandalf,
                "https://www.youtube.com/watch?v=G1IbRujko-A",
            ),
            VideoSource(
                1,
                R.raw.gandalf_2,
                "https://www.youtube.com/watch?v=ZIKvC9338rs",
            ),
            VideoSource(
                2,
                R.raw.gimli_sax,
                "https://www.youtube.com/watch?v=rBHpf6yM7QI",
            ),
            VideoSource(
                3,
                R.raw.gimlis_laugh,
                "https://www.youtube.com/watch?v=nSDHmZHR030",
            ),
            VideoSource(
                4,
                R.raw.theodens_laugh,
                "https://www.youtube.com/watch?v=hMZSipAcckw",
            ),
            VideoSource(
                5,
                R.raw.taking_the_hobbits_to_isengard,
                "https://www.youtube.com/watch?v=z9Uz1icjwrM",
            ),
        )
    }
}
