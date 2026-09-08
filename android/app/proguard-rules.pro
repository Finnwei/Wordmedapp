# El puente entre el cuadernillo y la app se llama desde JavaScript:
# si R8 le cambia el nombre a los métodos, el WebView no los encuentra.
-keepclassmembers class ar.wordmed.app.ui.PuenteLector {
    public *;
}
-keepattributes JavascriptInterface

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ar.wordmed.app.datos.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
