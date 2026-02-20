
plugins {
    `java-gradle-plugin`
    `maven-publish`
    jacoco
    id("com.diffplug.spotless") version "6.25.0"
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "io.github.namanoncode"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    website.set("https://github.com/namanONcode/Anchor-pq")
    vcsUrl.set("https://github.com/namanONcode/Anchor-pq.git")

    plugins {
        create("anchorPQIntegrity") {
            id = "io.github.namanoncode.anchorpq"
            implementationClass = "com.anchorpq.AnchorPQPlugin"
            displayName = "Anchor PQ Integrity Plugin"
            description = "Post-quantum integrity verification plugin for Android applications " +
                "using Merkle trees and ML-KEM (CRYSTALS-Kyber)"
            tags.set(
                listOf(
                    "android",
                    "security",
                    "integrity",
                    "merkle-tree",
                    "post-quantum",
                    "ml-kem",
                    "kyber",
                    "cryptography",
                ),
            )
        }
    }
}

dependencies {
    // Bouncy Castle for ML-KEM (CRYSTALS-Kyber) support
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")

    // Android Gradle Plugin for AGP integration
    compileOnly("com.android.tools.build:gradle:9.0.1")

    // Gson for JSON handling
    implementation("com.google.code.gson:gson:2.13.2")

    // Testing
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<Javadoc> {
    options {
        (this as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            encoding = "UTF-8"
        }
    }
    isFailOnError = false
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.namanoncode"
            artifactId = "anchorpq"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Anchor PQ Integrity Plugin")
                description.set(
                    "Post-quantum integrity verification plugin for Android applications " +
                        "using Merkle trees and ML-KEM (CRYSTALS-Kyber)",
                )
                url.set("https://github.com/namanONcode/Anchor-pq")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("namanoncode")
                        name.set("namanoncode")
                        url.set("https://github.com/namanoncode")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/namanONcode/Anchor-pq.git")
                    developerConnection.set("scm:git:ssh://github.com/namanONcode/Anchor-pq.git")
                    url.set("https://github.com/namanONcode/Anchor-pq")
                }
            }
        }
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.19.2")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.1.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
