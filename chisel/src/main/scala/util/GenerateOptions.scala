package util

case class GenerateOptions(
                            enableDebugPorts: Boolean,
                            useIP: Boolean,

                            systemClock: Int,
                            standardClock: Int,
                            uartClock: Int,
                            vgaClock: Int,
                          )
