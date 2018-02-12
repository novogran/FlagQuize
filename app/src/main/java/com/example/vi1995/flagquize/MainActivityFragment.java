package com.example.vi1995.flagquize;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivityFragment extends Fragment {

    private static final String TAG = "FlagQuiz Activity";
    private static final int FLAG_IN_QUIZ = 10;

    private List<String> fileNameList;
    private List<String> quizCountriesList;
    private Set<String> regionsSet;
    private String correctAnswer;
    private int totalGuesses;
    private int correctAnswers;
    private int guessRows;
    private SecureRandom random;
    private Handler handler;
    private Animation shakeAnimation;

    private LinearLayout quizLinearLayout;
    private TextView questionNumberTextView;
    private ImageView flagImageView;
    private LinearLayout[] guessLinearLayouts;
    private TextView answerTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);

        View view =  inflater.inflate(R.layout.fragment_main, container, false);
        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        quizLinearLayout = view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = view.findViewById(R.id.questionNumberTextView);
        flagImageView = view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = view.findViewById(R.id.row4LinearLayout);
        answerTextView = view.findViewById(R.id.answerTextView);

        for(LinearLayout row:guessLinearLayouts){
            for(int column=0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        questionNumberTextView.setText(getString(R.string.question,1,FLAG_IN_QUIZ));
        return view;
    }

    public void updateGuessRows(SharedPreferences sharedPreferences){
        String choises = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choises) / 2;

        for(LinearLayout layout: guessLinearLayouts)
            layout.setVisibility(View.GONE);

        for(int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    public void resetQuiz(){
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();

        try {
            for (String region : regionsSet) {
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        }

        catch (IOException exeption){
            Log.e(TAG, "Error loading image file names", exeption);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOffFlags = fileNameList.size();

        while (flagCounter <= FLAG_IN_QUIZ){
            int randomIndex = random.nextInt(numberOffFlags);

            String filename = fileNameList.get(randomIndex);

            if(!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }

        loadNextFlag();
    }

    private void loadNextFlag(){
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");

        questionNumberTextView.setText(getString(
                R.string.question, (correctAnswers + 1), FLAG_IN_QUIZ));

        String region = nextImage.substring(0, nextImage.indexOf('-'));
        AssetManager assets = getActivity().getAssets();

        try(InputStream stream = assets.open(region + "/" + nextImage + ".png")){
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false);
        }
        catch (IOException exeption){
            Log.e(TAG, "Error loadin" + nextImage, exeption);
        }

        Collections.shuffle(fileNameList);

        int corrent = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(corrent));

        for(int row = 0; row < guessRows; row++){
            for(int column = 0;
                    column < guessLinearLayouts[row].getChildCount(); column++) {
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        int row = random.nextInt(guessRows);
        int column = random.nextInt(2);
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    private String getCountryName(String name){
        return name.substring(name.indexOf('-') + 1).replace('-',' ');
    }

    private void animate(boolean animateOut) {

        if (correctAnswers == 0)
            return;

        int centerX = (quizLinearLayout.getLeft() +
                quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() +
                quizLinearLayout.getBottom()) / 2;
        int radius = Math.max(quizLinearLayout.getWidth(),
                quizLinearLayout.getHeight());

         Animator animator;

        if (animateOut) {
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextFlag();
                        }
                    }
            );
        }
        else {
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, 0, radius);
            }

        animator.setDuration(500);
        animator.start();
        }

        private View.OnClickListener guessButtonListener = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Button guessButton = ((Button) v);
                String guess = guessButton.getText().toString();
                String answer = getCountryName(correctAnswer);
                ++totalGuesses;

                if(guess.equals(answer)){

                    ++correctAnswers;

                    answerTextView.setText(answer + "!");
                    answerTextView.setTextColor(getResources().getColor(
                            R.color.corrent_answer));

                    disableButtons();

                    if(correctAnswers == FLAG_IN_QUIZ){

                        DialogFragment quizResults = new DialogFragment(){
                            @NonNull
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                return new AlertDialog.Builder(getActivity())
                                        .setMessage(getString(R.string.result,totalGuesses,
                                                (1000/(double)totalGuesses)))
                                        .setPositiveButton(R.string.restarting_quiz,
                                       new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                resetQuiz();
                                            }
                                        })
                                        .create();
                            }};



                        quizResults.setCancelable(false);
                        quizResults.show(getFragmentManager(), "quiz results");
                    }

                    else {
                        handler.postDelayed(
                                () -> animate(true), 2000);
                    }
                }
                else {
                    flagImageView.startAnimation(shakeAnimation);

                    answerTextView.setText(R.string.incorrect_answer);
                    answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
                    guessButton.setEnabled(false);
                }
            }
        };

    private void disableButtons(){
        for(int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for(int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
}
