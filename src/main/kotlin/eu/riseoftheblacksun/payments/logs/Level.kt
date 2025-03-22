package eu.riseoftheblacksun.payments.logs

enum class Level {
    DEBUG,
    INFO,
    WARN,
    ERROR;

    val prefix: String
        get() = "[${this.name.uppercase()}]"
}