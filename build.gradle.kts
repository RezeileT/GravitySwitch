import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

korge {
	id = "com.sample.demo"

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
	targetJvm()
	//targetJs()
    //targetWasm()
	targetDesktop()
	//targetIos()
	//targetAndroid()

	serializationJson()
}


dependencies {
    add("commonMainApi", project(":deps"));
    //add("commonMainApi", project(":korge-dragonbones"))
    //add("commonMainApi", "org.xerial:sqlite-jdbc:3.52.0")
    add("jvmMainApi", "org.xerial:sqlite-jdbc:3.48.0.0")
}

