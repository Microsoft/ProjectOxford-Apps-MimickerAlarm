/*
 *
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license.
 *
 * Project Oxford: http://ProjectOxford.ai
 *
 * Project Oxford Mimicker Alarm Github:
 * https://github.com/Microsoft/ProjectOxford-Apps-MimickerAlarm
 *
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.microsoft.mimickeralarm.mimics;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microsoft.mimickeralarm.R;
import com.microsoft.mimickeralarm.utilities.Loggable;
import com.microsoft.mimickeralarm.utilities.Logger;
import com.microsoft.mimickeralarm.utilities.KeyUtilities;
import com.microsoft.mimickeralarm.utilities.NewVisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalyzeResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * The logic and UI implementation of the Color capture mimic game
 *
 * See MimicWithCameraFragment for details of the camera interacts with the UI
 *
 * In vision_question.xml we defined a bunch of color and their acceptable HSL value ranges
 * see https://en.wikipedia.org/wiki/HSL_and_HSV for HSL color space
 *
 * On creation a random color is selected.
 * when an image is capture it is sent to the ProjectOxford Vision API which returns the main colors
 * and accent colors. Accent colors are defined as HEX codes of RGB values which we turn to HSL and
 * compare to see if it's in range of the color we specified.
 *
 */
public class MimicColorCaptureFragment extends MimicWithCameraFragment {
    private NewVisionServiceRestClient mVisionServiceRestClient;
    private String mQuestionColorName;
    private float[] mQuestionColorRangeLower;
    private float[] mQuestionColorRangeUpper;

    @SuppressWarnings("deprecation")
    public MimicColorCaptureFragment() {
        CameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Resources resources = getResources();

        String subscriptionKey = KeyUtilities.getToken(getActivity(), "vision");
        mVisionServiceRestClient = new NewVisionServiceRestClient(subscriptionKey);

        String[] questions = resources.getStringArray(R.array.vision_color_questions);
        TextView instruction = (TextView) view.findViewById(R.id.instruction_text);
        mQuestionColorName = questions[new Random().nextInt(questions.length)];
        instruction.setText(String.format(resources.getString(R.string.mimic_vision_prompt), mQuestionColorName));

        TypedArray colorCodeLower = resources.obtainTypedArray(resources.getIdentifier(mQuestionColorName + "_range_lower", "array", getActivity().getPackageName()));
        mQuestionColorRangeLower = new float[]{colorCodeLower.getFloat(0, 0f), colorCodeLower.getFloat(1, 0f), colorCodeLower.getFloat(2, 0f)};
        colorCodeLower.recycle();
        TypedArray colorCodeUpper = resources.obtainTypedArray(resources.getIdentifier(mQuestionColorName + "_range_upper", "array", getActivity().getPackageName()));
        mQuestionColorRangeUpper = new float[]{colorCodeUpper.getFloat(0, 0f), colorCodeUpper.getFloat(1, 0f), colorCodeUpper.getFloat(2, 0f)};
        colorCodeUpper.recycle();

        Logger.init(getActivity());
        Loggable playGameEvent = new Loggable.UserAction(Loggable.Key.ACTION_GAME_COLOR);
        Logger.track(playGameEvent);

        return view;
    }

    @Override
    public GameResult verify(Bitmap bitmap) {
        GameResult gameResult = new GameResult();
        gameResult.question = ((TextView) getView().findViewById(R.id.instruction_text)).getText().toString();

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
            String[] features = {"Color"};
            Loggable.AppAction appAction = new Loggable.AppAction(Loggable.Key.APP_API_VISION);
            Logger.trackDurationStart(appAction);
            AnalyzeResult result = mVisionServiceRestClient.analyzeImage(inputStream, features);
            Logger.track(appAction);

            float[] accentHsl = new float[3];
            int[] accentRgb = hexStringToRgb(result.color.accentColor);
            ColorUtils.RGBToHSL(accentRgb[0], accentRgb[1], accentRgb[2], accentHsl);
            boolean colorInRange = isColorInRange(mQuestionColorRangeLower, mQuestionColorRangeUpper, accentHsl);
            Loggable.UserAction userAction = new Loggable.UserAction(Loggable.Key.ACTION_GAME_COLOR_SUCCESS);
            userAction.putProp(Loggable.Key.PROP_QUESTION, mQuestionColorName);
            userAction.putVision(result);

            //TODO: this will not work for languages other than English.
            if (colorInRange ||
                    result.color.dominantColorForeground.toLowerCase().equals(mQuestionColorName) ||
                    result.color.dominantColorBackground.toLowerCase().equals(mQuestionColorName)) {
                gameResult.success = true;
            }

            for (String color : result.color.dominantColors) {
                if (color.toLowerCase().equals(mQuestionColorName)) {
                    gameResult.success = true;
                    break;
                }
            }

            if (!gameResult.success) {
                userAction.Name = Loggable.Key.ACTION_GAME_COLOR_FAIL;
            }

            Logger.track(userAction);
        } catch (Exception ex) {
            Logger.trackException(ex);
        }

        return gameResult;
    }

    @Override
    protected void gameFailure(GameResult gameResult, boolean allowRetry) {
        if (!allowRetry) {
            Loggable.UserAction userAction = new Loggable.UserAction(Loggable.Key.ACTION_GAME_COLOR_TIMEOUT);
            userAction.putProp(Loggable.Key.PROP_QUESTION, mQuestionColorName);
            Logger.track(userAction);
        }
        super.gameFailure(gameResult, allowRetry);
    }

    private int[] hexStringToRgb(String hex) {
        int color = (int) Long.parseLong(hex, 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        return new int[]{r, g, b};
    }

    private boolean isColorInRange(float[] lowerHsl, float[] upperHsl, float[] queryHsl) {
        boolean result = true;
        for (int i = 0; i < 3; i++) {
            //looped around the color wheel
            if (upperHsl[i] < lowerHsl[i]) {
                result &= (queryHsl[i] >= lowerHsl[i] && queryHsl[i] <= 360)
                        || (queryHsl[i] >= 0 && queryHsl[i] <= upperHsl[i]);
            } else {
                result &= (queryHsl[i] >= lowerHsl[i] && queryHsl[i] <= upperHsl[i]);
            }
        }

        Logger.local("HSL 1: " + lowerHsl[0] + " " + lowerHsl[1] + " " + lowerHsl[2]);
        Logger.local("HSL 2: " + upperHsl[0] + " " + upperHsl[1] + " " + upperHsl[2]);
        Logger.local("question HSL 2: " + queryHsl[0] + " " + queryHsl[1] + " " + queryHsl[2]);
        Logger.local("result: " + result);

        return result;
    }
}
