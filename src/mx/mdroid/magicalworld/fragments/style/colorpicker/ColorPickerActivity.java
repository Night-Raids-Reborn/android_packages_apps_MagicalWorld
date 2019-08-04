package mx.mdroid.magicalworld.fragments.style.colorpicker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.ContextThemeWrapper;
import android.view.Window;
import android.view.WindowManager;

import com.android.settings.R;
import mx.mdroid.magicalworld.fragments.style.util.OverlayManager;

public class ColorPickerActivity extends Activity {

    private static final String ACCENT_COLOR_PROP = "persist.sys.theme.accentcolor";

    private int mColor;
    private boolean mHexValueEnable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        setContentView(R.layout.color_picker);

        String colorVal = SystemProperties.get(ACCENT_COLOR_PROP, "-1");
        mColor = "-1".equals(colorVal)
                ? Color.WHITE
                : Color.parseColor("#" + colorVal);

        new ColorPickerDialog.Builder(new ContextThemeWrapper(ColorPickerActivity.this, R.style.ColorPickerDialogTheme), mColor)
                .setHexValueEnabled(mHexValueEnable)
                .setOnColorPickedListener(new ColorPickerDialog.OnColorPickedListener() {
                    @Override
                    public void onColorPicked(int color) {
                        mColor = color;
                        String hexColor = String.format("%08X", (0xFFFFFFFF & mColor));
                        SystemProperties.set(ACCENT_COLOR_PROP, hexColor);
                        OverlayManager om = new OverlayManager(ColorPickerActivity.this);
                        om.reloadAndroidAssets();
                        om.reloadAssets("com.android.settings");
                        om.reloadAssets("com.android.systemui");
                    }
                })
                .build()
                .show();
    }
}
