package com.frozeninferno.client.application.home;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.ViewImpl;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.extras.animate.client.ui.Animate;
import org.gwtbootstrap3.extras.animate.client.ui.constants.Animation;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.select.client.ui.Select;
import org.gwtbootstrap3.extras.toggleswitch.client.ui.ToggleSwitch;

import javax.inject.Inject;
import java.util.List;

/**
 * Home view class
 */
public class HomeView extends ViewImpl implements HomePresenter.MyView {

    /**
     * UI Fields
     */
    @UiField
    TextArea urlTextArea;
    @UiField
    Button downloadButton;
    @UiField
    TextArea outputTextArea;
    @UiField
    PanelBody urlBody;
    @UiField
    LinkedGroup urlGroup;
    @UiField
    Select audioFormatSelect;
    @UiField
    ToggleSwitch audioToggleSwitch;
    @UiField
    Paragraph navbarParagraph;
    @UiField
    ToggleSwitch ignoreErrorsToggleSwitch;

    /**
     * Whether downloading is disabled
     */
    private boolean downloadDisabled = false;

    @Inject
    HomeView(Binder uiBinder) {
        initWidget(uiBinder.createAndBindUi(this));
        navbarParagraph.setHTML(navbarParagraph.getHTML().replace("YouTube", "YouTube <a href=\"https://github.com/rg3/youtube-dl/blob/master/docs/supportedsites.md\" target=\"_blank\">(and other sites)</a>"));
    }

    /**
     * Gets the selected audio format
     *
     * @return Selected audio format
     */
    @Override
    public String getAudioFormat() {
        if (audioFormatSelect.getSelectedItem() != null) {
            return audioFormatSelect.getSelectedItem().getValue();
        }

        return "";
    }

    /**
     * Toggles the download button on or off
     *
     * @param toggle True for on
     */
    @Override
    public void toggleDownloadButton(boolean toggle) {
        downloadButton.setEnabled(toggle);
        if (!toggle) {
            Animate.animate(downloadButton, Animation.PULSE);
            downloadButton.setText("Downloading...");
        } else {
            downloadButton.setText("Download");
        }
    }

    /**
     * Set links to files
     *
     * @param groupItems Files to add to group
     */
    @Override
    public void setLinkItems(final List<LinkedGroupItem> groupItems) {
        urlGroup.clear();
        for (LinkedGroupItem groupItem : groupItems) {
            urlGroup.add(groupItem);
        }
    }

    /**
     * Get the text from the url text area
     *
     * @return URLs from text area
     */
    @Override
    public String getUrls() {
        return urlTextArea.getText();
    }

    /**
     * Add a click handler to the download button
     *
     * @param handler New click handler to add
     */
    @Override
    public void addButtonClickHandler(ClickHandler handler) {
        downloadButton.addClickHandler(handler);
    }

    /**
     * Disable downloading when storage limit reached.
     */
    @Override
    public void disableDownload() {
        downloadButton.setEnabled(false);
        downloadButton.setText("Storage limit reached...");
        if (!downloadDisabled) {
            Bootbox.alert("Storage limit reached. You may download existing files, but won't be allowed to add more.");
            downloadDisabled = true;
        }
    }

    /**
     * Set the console output text
     *
     * @param consoleOut Output of youtube-dl download
     */
    @Override
    public void setOutputText(String consoleOut) {
        outputTextArea.setText(consoleOut);
    }

    /**
     * Get whether to convert videos to audio
     *
     * @return True if yes
     */
    @Override
    public boolean getConvertAudio() {
        return audioToggleSwitch.getValue();
    }

    /**
     * Get whether to ignore errors during download
     *
     * @return True if yes
     */
    @Override
    public boolean getIgnoreErrors() {
        return ignoreErrorsToggleSwitch.getValue();
    }

    interface Binder extends UiBinder<Widget, HomeView> {
    }
}