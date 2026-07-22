plugins {
    id("dev.kikugie.stonecutter")
}

// Safety for swaps
val ci: String? = System.getenv("CI")
if (ci != null) sc active null else stonecutter active "1.21.4"
