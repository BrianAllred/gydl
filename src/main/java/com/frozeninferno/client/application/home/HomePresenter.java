package com.frozeninferno.client.application.home;

import com.frozeninferno.client.application.ApplicationPresenter;
import com.frozeninferno.client.place.NameTokens;
import com.frozeninferno.shared.UUID;
import com.frozeninferno.shared.YDLService;
import com.frozeninferno.shared.YDLServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.query.client.Function;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.gwtbootstrap3.client.ui.LinkedGroupItem;
import org.gwtbootstrap3.client.ui.constants.ListGroupItemType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.gwt.query.client.GQuery.$;
import static com.google.gwt.query.client.GQuery.window;

/**
 * Presenter class
 */
public class HomePresenter extends Presenter<HomePresenter.MyView, HomePresenter.MyProxy> {

    /**
     * UUID of session
     */
    private final String uuid = UUID.uuid();

    /**
     * Timer for retrieving files
     */
    private final Timer timer;

    /**
     * Client-side view
     */
    public MyView view;

    /**
     * Service implementation
     */
    private YDLServiceAsync ydlService;

    /**
     * Whether the servlet is currently downloading files
     */
    private boolean downloading;

    @Inject
    HomePresenter(
            final EventBus eventBus,
            final MyView view,
            final MyProxy proxy) {
        super(eventBus, view, proxy, ApplicationPresenter.SLOT_MAIN);
        this.view = view;

        // Create service
        ydlService = GWT.create(YDLService.class);

        // Send our unique id to the service
        ydlService.sendId(uuid, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Error communicating with server, try again or refresh page.");
            }

            @Override
            public void onSuccess(String result) {
                view.setOutputText(result);
            }
        });

        // Add a new click handler to the download button
        view.addButtonClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // Disable button during downloads
                view.toggleDownloadButton(false);
                HomePresenter.this.downloading = true;

                // Get urls
                List<String> urls = new ArrayList<>(Arrays.asList(view.getUrls().split("\n")));
                /*for (String url : view.getUrls().split("\n")) {
                    urls.add(url);
                }*/

                // Set the urls
                ydlService.setURLs(HomePresenter.this.uuid, urls, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Error: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        List<String> options = new ArrayList<>();

                        // Add various options
                        if (view.getConvertAudio()) {
                            options.add("-x");
                        }

                        if (view.getIgnoreErrors()) {
                            options.add("-i");
                        }

                        options.add("--audio-format");
                        switch (view.getAudioFormat()) {
                            case "":
                            case "best":
                                options.add("best");
                                break;
                            default:
                                options.add(view.getAudioFormat());
                                break;
                        }

                        // Set the options
                        ydlService.setOptions(HomePresenter.this.uuid, options, new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                // Start the download
                                ydlService.startDownload(HomePresenter.this.uuid, new AsyncCallback<String>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        Window.alert("Error: " + caught.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(String result) {
                                        // Set the console text
                                        view.setOutputText(result);

                                        // Re-enable the download button
                                        view.toggleDownloadButton(true);
                                        HomePresenter.this.downloading = false;
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        // Register for the before unload event
        $(window).on("beforeunload", new Function() {
            public void f() {
                // Tell the service we're done
                ydlService.unload(HomePresenter.this.uuid, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        view.setOutputText(caught.getMessage());
                        Window.alert(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {

                    }
                });
            }
        });

        // Setup timer for getting size and files
        timer = new Timer() {
            @Override
            public void run() {
                ydlService.getSizeLimitReached(HomePresenter.this.uuid, new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        view.setOutputText(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            view.disableDownload();
                        }

                        if (!HomePresenter.this.downloading) {
                            ydlService.getFiles(HomePresenter.this.uuid, new AsyncCallback<List<String>>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    view.setOutputText(caught.getMessage());
                                }

                                @Override
                                public void onSuccess(List<String> result) {
                                    List<LinkedGroupItem> groupItems = new ArrayList<>();
                                    for (String file : result) {
                                        if (file.substring(file.lastIndexOf('.') + 1) == "part") {
                                            continue;
                                        }

                                        final String url = GWT.getModuleBaseURL() + "YDLService?fileInfo1=" + file;
                                        final LinkedGroupItem groupItem = new LinkedGroupItem(file.substring(file.lastIndexOf('/') + 1), "#");
                                        groupItem.setType(ListGroupItemType.INFO);
                                        groupItem.addClickHandler(new ClickHandler() {
                                            @Override
                                            public void onClick(ClickEvent event) {
                                                Window.open(url, "_blank", "");
                                            }
                                        });

                                        groupItems.add(groupItem);
                                    }

                                    if (!groupItems.isEmpty()) {
                                        view.setLinkItems(groupItems);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        };

        // Fire it off every second
        timer.scheduleRepeating(1000);
    }

    // Define interface for the view
    interface MyView extends View {
        String getUrls();

        void addButtonClickHandler(final ClickHandler handler);

        void setOutputText(String s);

        void toggleDownloadButton(boolean toggle);

        void setLinkItems(final List<LinkedGroupItem> groupItems);

        boolean getConvertAudio();

        String getAudioFormat();

        void disableDownload();

        boolean getIgnoreErrors();
    }

    @ProxyStandard
    @NameToken(NameTokens.HOME)
    interface MyProxy extends ProxyPlace<HomePresenter> {
    }
}