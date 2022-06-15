module Security {
    requires java.desktop;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;
    requires org.jetbrains.annotations;
    requires miglayout;
    requires Image;

    opens com.udacity.catpoint.data to com.google.gson;
}