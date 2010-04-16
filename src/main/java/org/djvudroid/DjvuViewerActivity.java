package org.djvudroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import org.djvudroid.codec.LibraryExtracter;
import org.djvudroid.models.ZoomModel;
import org.djvudroid.views.DjvuZoomControls;

public class DjvuViewerActivity extends Activity
{
    private static final int MENU_EXIT = 0;
    private static final int MENU_GOTO = 1;
    private static final int MENU_FULL_SCREEN = 2;
    private static final int MENU_PAGE_LAYOUT = 3;

    private static final int DIALOG_GOTO = 0;

    private static final String DOCUMENT_VIEW_STATE_PREFERENCES = "DjvuDocumentViewState";
    private static DecodeService decodeService;

    //Reuse decodeService in process cause it holds decode caches
    private DjvuDocumentView documentView;
    private ViewerPreferences viewerPreferences;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LibraryExtracter.extractCodecLibrary(this);
        initDecodeService();
        final ZoomModel zoomModel = new ZoomModel();
        documentView = new DjvuDocumentView(this, zoomModel);
        zoomModel.addEventListener(documentView);
        documentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        decodeService.setContentResolver(getContentResolver());
        decodeService.setContainerView(documentView);
        documentView.setDecodeService(decodeService);
        decodeService.open(getIntent().getData());

        viewerPreferences = new ViewerPreferences(this);

        final FrameLayout frameLayout = createMainContainer();
        frameLayout.addView(documentView);
        frameLayout.addView(createZoomControls(zoomModel));
        setFullScreen();
        setContentView(frameLayout);

        final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        documentView.goToPage(sharedPreferences.getInt(getIntent().getData().toString(), 0));
        decodeService.twoUp = sharedPreferences.getBoolean(getIntent().getData().toString() + ".twoUp", false);
        documentView.showDocument();

        viewerPreferences.addRecent(getIntent().getData());
    }

    private void setFullScreen()
    {
        if (viewerPreferences.isFullScreen())
        {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private DjvuZoomControls createZoomControls(ZoomModel zoomModel)
    {
        final DjvuZoomControls controls = new DjvuZoomControls(this, zoomModel);
        controls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        zoomModel.addEventListener(controls);
        return controls;
    }

    private FrameLayout createMainContainer()
    {
        return new FrameLayout(this);
    }

    private static void initDecodeService()
    {
        if (decodeService == null)
        {
            decodeService = new DecodeService();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        saveCurrentPage();
    }

    private void saveCurrentPage()
    {
        final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getIntent().getData().toString(), documentView.getCurrentPage());
        editor.putBoolean(getIntent().getData().toString() + ".twoUp", decodeService.twoUp);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_EXIT, 0, "Exit");
        menu.add(0, MENU_GOTO, 0, "Go to page");
        final MenuItem menuItem = menu.add(0, MENU_FULL_SCREEN, 0, "Full screen").setCheckable(true).setChecked(viewerPreferences.isFullScreen());
        setFullScreenMenuItemText(menuItem);
        menu.add(0, MENU_PAGE_LAYOUT, 0, "Toggle page layout");
        return true;
    }

    private void setFullScreenMenuItemText(MenuItem menuItem)
    {
        menuItem.setTitle("Full screen " + (menuItem.isChecked() ? "on" : "off"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_EXIT:
                saveCurrentPage();
                System.exit(0);
                return true;
            case MENU_GOTO:
                showDialog(DIALOG_GOTO);
                return true;
            case MENU_FULL_SCREEN:
                item.setChecked(!item.isChecked());
                setFullScreenMenuItemText(item);
                viewerPreferences.setFullScreen(item.isChecked());
                
                finish();
                startActivity(getIntent());
                return true;
            case MENU_PAGE_LAYOUT:
	        decodeService.twoUp = !decodeService.twoUp;
		documentView.setDecodeService(decodeService);
		documentView.showDocument();
		return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case DIALOG_GOTO:
                return new GoToPageDialog(this, documentView, decodeService);
        }
        return null;
    }
}
