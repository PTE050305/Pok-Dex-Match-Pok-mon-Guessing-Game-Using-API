package com.example.majoractivity4;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.majoractivity4.api.PokeApiService;
import com.example.majoractivity4.model.PokemonResponse;
import com.example.majoractivity4.util.PokemonNameBank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int TOTAL_ROUNDS = 5;
    private static final int MAX_POKEMON_ID = 151;
    private static final long OVERLAY_DELAY_MS = 3000L;

    private ImageView ivPokemon;
    private ScrollView screenRoot;
    private FrameLayout headerPanel;
    private FrameLayout gameCompleteOverlay;
    private TextView tvResult;
    private TextView tvScore;
    private TextView tvRound;
    private TextView tvPokemonName;
    private TextView tvPokemonNumber;
    private TextView tvTypeBadge1;
    private TextView tvTypeBadge2;
    private TextView tvFinalScoreOverlay;
    private Button btnNextPokemon;
    private Button btnStartAnotherGame;
    private Button[] answerButtons;

    private final Random random = new Random();
    private PokeApiService apiService;

    private String correctPokemonName;
    private int score = 0;
    private int currentRound = 1;
    private boolean answeredCurrentRound = false;
    private boolean gameFinished = false;
    private int currentBackgroundColor = Color.parseColor("#7EA5EA");
    private final Runnable showOverlayRunnable = this::showGameCompleteOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initApi();
        startNewGame();
    }

    private void initViews() {
        screenRoot = findViewById(R.id.screenRoot);
        headerPanel = findViewById(R.id.headerPanel);
        gameCompleteOverlay = findViewById(R.id.gameCompleteOverlay);
        ivPokemon = findViewById(R.id.ivPokemon);
        tvResult = findViewById(R.id.tvResult);
        tvScore = findViewById(R.id.tvScore);
        tvRound = findViewById(R.id.tvRound);
        tvPokemonName = findViewById(R.id.tvPokemonName);
        tvPokemonNumber = findViewById(R.id.tvPokemonNumber);
        tvTypeBadge1 = findViewById(R.id.tvTypeBadge1);
        tvTypeBadge2 = findViewById(R.id.tvTypeBadge2);
        tvFinalScoreOverlay = findViewById(R.id.tvFinalScoreOverlay);
        btnNextPokemon = findViewById(R.id.btnNextPokemon);
        btnStartAnotherGame = findViewById(R.id.btnStartAnotherGame);

        answerButtons = new Button[] {
                findViewById(R.id.btnAnswer1),
                findViewById(R.id.btnAnswer2),
                findViewById(R.id.btnAnswer3),
                findViewById(R.id.btnAnswer4)
        };

        for (Button button : answerButtons) {
            button.setOnClickListener(v -> checkAnswer(((Button) v).getText().toString()));
        }

        btnNextPokemon.setOnClickListener(v -> {
            if (gameFinished) {
                startNewGame();
                return;
            }

            currentRound++;
            loadRandomPokemon();
        });

        btnStartAnotherGame.setOnClickListener(v -> startNewGame());
    }

    private void initApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(PokeApiService.class);
    }

    private void startNewGame() {
        screenRoot.removeCallbacks(showOverlayRunnable);
        score = 0;
        currentRound = 1;
        gameFinished = false;
        answeredCurrentRound = false;
        updateScoreText();
        updateRoundText();
        tvResult.setText(R.string.loading_pokemon);
        btnNextPokemon.setText(R.string.next_pokemon);
        btnNextPokemon.setVisibility(View.GONE);
        gameCompleteOverlay.setVisibility(View.GONE);
        loadRandomPokemon();
    }

    private void loadRandomPokemon() {
        if (currentRound > TOTAL_ROUNDS) {
            gameFinished = true;
            scheduleGameCompleteOverlay();
            return;
        }

        answeredCurrentRound = false;
        correctPokemonName = null;
        tvResult.setText(R.string.loading_pokemon);
        updateRoundText();
        setAnswerButtonsEnabled(false);
        btnNextPokemon.setVisibility(View.GONE);

        int randomId = random.nextInt(MAX_POKEMON_ID) + 1;
        apiService.getPokemonById(randomId).enqueue(new Callback<PokemonResponse>() {
            @Override
            public void onResponse(@NonNull Call<PokemonResponse> call, @NonNull Response<PokemonResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getName() == null) {
                    showLoadError();
                    return;
                }

                PokemonResponse pokemon = response.body();
                correctPokemonName = pokemon.getName().toLowerCase(Locale.US);
                String imageUrl = pokemon.getBestImageUrl();
                bindPokemonCard(pokemon);

                if (imageUrl == null || imageUrl.isEmpty()) {
                    tvResult.setText(R.string.image_load_error);
                } else {
                    tvResult.setText(R.string.choose_answer);
                }

                loadPokemonImage(imageUrl);
                showAnswerChoices(correctPokemonName);
                setAnswerButtonsEnabled(true);
            }

            @Override
            public void onFailure(@NonNull Call<PokemonResponse> call, @NonNull Throwable t) {
                showLoadError();
            }
        });
    }

    private void loadPokemonImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher_round)
                .listener(new RequestListener<>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        tvResult.setText(R.string.image_load_error);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(ivPokemon);
    }

    private void showAnswerChoices(String correctName) {
        List<String> choices = new ArrayList<>();
        choices.add(correctName);

        List<String> allNames = new ArrayList<>(Arrays.asList(PokemonNameBank.GEN1_POKEMON_NAMES));
        allNames.remove(correctName);
        Collections.shuffle(allNames);

        for (int i = 0; i < 3; i++) {
            choices.add(allNames.get(i));
        }
        Collections.shuffle(choices);

        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(formatPokemonName(choices.get(i)));
        }
    }

    private void checkAnswer(String selectedAnswer) {
        if (answeredCurrentRound || correctPokemonName == null) {
            return;
        }

        answeredCurrentRound = true;
        setAnswerButtonsEnabled(false);
        btnNextPokemon.setVisibility(View.VISIBLE);

        String normalizedSelection = selectedAnswer.toLowerCase(Locale.US).replace(" ", "-");
        String formattedCorrectName = formatPokemonName(correctPokemonName);
        if (normalizedSelection.equals(correctPokemonName)) {
            score++;
            tvResult.setText(R.string.correct_answer);
        } else {
            tvResult.setText(getString(R.string.wrong_answer_with_name, formattedCorrectName));
        }

        updateScoreText();

        if (currentRound == TOTAL_ROUNDS) {
            gameFinished = true;
            btnNextPokemon.setVisibility(View.GONE);
            scheduleGameCompleteOverlay();
        }
    }

    private void scheduleGameCompleteOverlay() {
        screenRoot.removeCallbacks(showOverlayRunnable);
        screenRoot.postDelayed(showOverlayRunnable, OVERLAY_DELAY_MS);
    }

    private void showGameCompleteOverlay() {
        setAnswerButtonsEnabled(false);
        btnNextPokemon.setVisibility(View.GONE);
        tvFinalScoreOverlay.setText(getString(R.string.final_score_format, score, TOTAL_ROUNDS));
        gameCompleteOverlay.setVisibility(View.VISIBLE);
    }

    private void bindPokemonCard(PokemonResponse pokemon) {
        String name = pokemon.getName() != null ? pokemon.getName() : getString(R.string.unknown_pokemon);
        tvPokemonName.setText(formatPokemonName(name));
        tvPokemonNumber.setText(getString(R.string.pokemon_number_format, pokemon.getId()));

        String primaryType = pokemon.getPrimaryType();
        String secondaryType = pokemon.getSecondaryType();
        bindTypeBadges(primaryType, secondaryType);

        int baseColor = getTypeColor(primaryType);
        animateThemeColor(baseColor);
    }

    private void bindTypeBadges(String primaryType, String secondaryType) {
        String safePrimary = primaryType == null ? "normal" : primaryType;
        tvTypeBadge1.setText(safePrimary.toUpperCase(Locale.US));
        tvTypeBadge1.setBackgroundTintList(ColorStateList.valueOf(getTypeColor(safePrimary)));

        if (secondaryType == null || secondaryType.isEmpty()) {
            tvTypeBadge2.setVisibility(View.GONE);
        } else {
            tvTypeBadge2.setVisibility(View.VISIBLE);
            tvTypeBadge2.setText(secondaryType.toUpperCase(Locale.US));
            tvTypeBadge2.setBackgroundTintList(ColorStateList.valueOf(getTypeColor(secondaryType)));
        }
    }

    private void animateThemeColor(int baseColor) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), currentBackgroundColor, baseColor);
        animator.setDuration(500);
        animator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();

            GradientDrawable header = new GradientDrawable(
                    GradientDrawable.Orientation.TR_BL,
                    new int[] { lightenColor(color, 0.22f), darkenColor(color, 0.08f) }
            );
            float radius = dpToPx(26);
            header.setCornerRadii(new float[] {
                    radius, radius,
                    radius, radius,
                    dpToPx(90), dpToPx(90),
                    dpToPx(90), dpToPx(90)
            });
            headerPanel.setBackground(header);
        });
        animator.start();
        currentBackgroundColor = baseColor;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private int getTypeColor(String typeName) {
        if (typeName == null) {
            return Color.parseColor("#6D8ED9");
        }

        switch (typeName.toLowerCase(Locale.US)) {
            case "water":
                return Color.parseColor("#4592E5");
            case "fire":
                return Color.parseColor("#F37D4E");
            case "grass":
                return Color.parseColor("#53BA6C");
            case "electric":
                return Color.parseColor("#E9BD3E");
            case "psychic":
            case "poison":
                return Color.parseColor("#A76BD8");
            case "ice":
                return Color.parseColor("#71C7D8");
            case "fighting":
                return Color.parseColor("#D87163");
            case "ground":
                return Color.parseColor("#BE9C5B");
            case "rock":
                return Color.parseColor("#A68A5A");
            case "dragon":
                return Color.parseColor("#6E7CE2");
            case "ghost":
                return Color.parseColor("#7579D8");
            case "dark":
                return Color.parseColor("#6C6062");
            case "bug":
                return Color.parseColor("#8CB85A");
            case "fairy":
                return Color.parseColor("#E79AC2");
            case "steel":
                return Color.parseColor("#7FA8BA");
            case "flying":
                return Color.parseColor("#7A9FE9");
            case "normal":
            default:
                return Color.parseColor("#6D8ED9");
        }
    }

    private int darkenColor(int color, float amount) {
        int r = (int) (Color.red(color) * (1f - amount));
        int g = (int) (Color.green(color) * (1f - amount));
        int b = (int) (Color.blue(color) * (1f - amount));
        return Color.rgb(Math.max(r, 0), Math.max(g, 0), Math.max(b, 0));
    }

    private int lightenColor(int color, float amount) {
        int r = (int) (Color.red(color) + ((255 - Color.red(color)) * amount));
        int g = (int) (Color.green(color) + ((255 - Color.green(color)) * amount));
        int b = (int) (Color.blue(color) + ((255 - Color.blue(color)) * amount));
        return Color.rgb(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    private String formatPokemonName(String rawName) {
        String[] parts = rawName.split("-");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            String part = parts[i];
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
            if (i < parts.length - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private void updateScoreText() {
        tvScore.setText(getString(R.string.score_format, score));
    }

    private void updateRoundText() {
        tvRound.setText(getString(R.string.round_format, currentRound, TOTAL_ROUNDS));
    }

    private void setAnswerButtonsEnabled(boolean enabled) {
        for (Button button : answerButtons) {
            button.setEnabled(enabled);
        }
    }

    private void showLoadError() {
        tvResult.setText(getString(R.string.failed_load_data) + "\n" + getString(R.string.check_internet));
        setAnswerButtonsEnabled(false);
        btnNextPokemon.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.failed_load_data, Toast.LENGTH_SHORT).show();
    }
}