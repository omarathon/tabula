// SBT build file.

name := "courses"

unmanagedJars in Test <<= baseDirectory map { base => (base/"lib"/"test" ** "*.jar").classpath }

unmanagedResourceDirectories in Test += file("src/main/webapp")

unmanagedJars in Compile <<= baseDirectory map { base =>
	val lib = base / "lib"
	val baseDirectories = (lib / "build" ) +++ (lib / "runtime")
	val customJars = (baseDirectories ** "*.jar")
	customJars.classpath
}
