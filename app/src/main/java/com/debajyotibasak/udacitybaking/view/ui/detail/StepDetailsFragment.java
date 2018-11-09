package com.debajyotibasak.udacitybaking.view.ui.detail;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.debajyotibasak.udacitybaking.R;
import com.debajyotibasak.udacitybaking.api.model.Step;
import com.debajyotibasak.udacitybaking.interfaces.StepButtonClickListener;
import com.debajyotibasak.udacitybaking.view.ui.MainViewModel;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class StepDetailsFragment extends DaggerFragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private MainViewModel mainViewModel;
    private StepButtonClickListener stepButtonClickListener;

    private SimpleExoPlayer simpleExoPlayer;
    private PlayerView exoPlayerView;
    private TextView txvDescription;
    private Button btnNext;
    private Button btnPrevious;

    private boolean playWhenReady;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    private static final String PLAYER_STATE = "player_state";
    private static final String PLAYER_POSITION = "key_player_position";
    private static final String PLAY_WHEN_READY = "key_play_when_ready";

    private Step step = null;

    public StepDetailsFragment() {
        // Required empty public constructor
    }

    public static StepDetailsFragment newInstance() {
        Bundle args = new Bundle();
        args.putBundle(PLAYER_STATE, null);
        StepDetailsFragment fragment = new StepDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void initViews(View view) {
        exoPlayerView = view.findViewById(R.id.exo_player);
        txvDescription = view.findViewById(R.id.txvDescription);
        btnNext = view.findViewById(R.id.btn_next);
        btnPrevious = view.findViewById(R.id.btn_prev);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof StepButtonClickListener) {
            stepButtonClickListener = (StepButtonClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement StepButtonClickListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(MainViewModel.class);

        if (getArguments() != null) {
            Bundle playerState = getArguments().getBundle(PLAYER_STATE);
            if (playerState != null) {
                playbackPosition = playerState.getLong(PLAYER_POSITION);
                playWhenReady = playerState.getBoolean(PLAY_WHEN_READY);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_step_details, container, false);
        initViews(view);

        mainViewModel.getSelectedStep().observe(getActivity(), selectedStep -> {
            if (selectedStep != null) {
                step = selectedStep;
                txvDescription.setText(step.getDescription());
            }
        });

        mainViewModel.getStepsSize().observe(this, stepSize -> {
            if (stepSize != null) {
                mainViewModel.getCurrentStep().observe(this, currentStep -> {
                    if (currentStep != null) {
                        if (currentStep - 1 < 0) {
                            btnPrevious.setVisibility(View.INVISIBLE);
                        } else if (currentStep + 1 > stepSize - 1) {
                            btnNext.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });

        btnNext.setOnClickListener(v -> stepButtonClickListener.onNextStep());
        btnPrevious.setOnClickListener(v -> stepButtonClickListener.onPreviousStep());

        return view;
    }

    private void initializePlayer() {
        String url = step.getVideoURL();

        if (url.isEmpty()) {
            exoPlayerView.setVisibility(View.GONE);
        }

        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(getActivity()),
                new DefaultTrackSelector(),
                new DefaultLoadControl()
        );

        exoPlayerView.setPlayer(simpleExoPlayer);

        MediaSource mediaSource = new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory(getActivity().getPackageName()))
                .createMediaSource(Uri.parse(url));

        simpleExoPlayer.prepare(mediaSource, true, false);
        simpleExoPlayer.setPlayWhenReady(playWhenReady);
        simpleExoPlayer.seekTo(currentWindow, playbackPosition);
    }

    private void releasePlayer() {
        if (simpleExoPlayer != null) {
            playbackPosition = simpleExoPlayer.getCurrentPosition();
            currentWindow = simpleExoPlayer.getCurrentWindowIndex();
            playWhenReady = simpleExoPlayer.getPlayWhenReady();
            simpleExoPlayer.release();
            simpleExoPlayer = null;

            if (getArguments() != null) {
                Bundle args = new Bundle();
                args.putLong(PLAYER_POSITION, playbackPosition);
                args.putBoolean(PLAY_WHEN_READY, playWhenReady);
                getArguments().putBundle(PLAYER_STATE, args);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) initializePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT > 23 || simpleExoPlayer == null) initializePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT > 23) releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) releasePlayer();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stepButtonClickListener = null;
    }
}