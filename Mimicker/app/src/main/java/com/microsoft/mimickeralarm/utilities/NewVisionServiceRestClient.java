package com.microsoft.mimickeralarm.utilities;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.contract.AnalyzeResult;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import com.microsoft.projectoxford.vision.rest.WebServiceRequest;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


public class NewVisionServiceRestClient implements VisionServiceClient {
    private static final String serviceHost = "https://api.projectoxford.ai/vision/v1.0";
    private WebServiceRequest restCall = null;
    private Gson gson = new Gson();

    public NewVisionServiceRestClient(String subscriptKey) {
        this.restCall = new WebServiceRequest(subscriptKey);
    }

    public AnalyzeResult analyzeImage(String url, String[] visualFeatures) throws VisionServiceException {
        HashMap params = new HashMap();
        String features = TextUtils.join(",", visualFeatures);
        params.put("visualFeatures", features);
        String path = "https://api.projectoxford.ai/vision/v1.0/analyze";
        String uri = WebServiceRequest.getUrl(path, params);
        params.clear();
        params.put("url", url);
        String json = (String)this.restCall.request(uri, "POST", params, (String)null, false);
        AnalyzeResult visualFeature = (AnalyzeResult)this.gson.fromJson(json, AnalyzeResult.class);
        return visualFeature;
    }

    public AnalyzeResult analyzeImage(InputStream stream, String[] visualFeatures) throws VisionServiceException, IOException {
        HashMap params = new HashMap();
        String features = TextUtils.join(",", visualFeatures);
        params.put("visualFeatures", features);
        String path = "https://api.projectoxford.ai/vision/v1.0/analyze";
        String uri = WebServiceRequest.getUrl(path, params);
        params.clear();
        byte[] data = IOUtils.toByteArray(stream);
        params.put("data", data);
        String json = (String)this.restCall.request(uri, "POST", params, "application/octet-stream", false);
        AnalyzeResult visualFeature = (AnalyzeResult)this.gson.fromJson(json, AnalyzeResult.class);
        return visualFeature;
    }

    public OCR recognizeText(String url, String languageCode, boolean detectOrientation) throws VisionServiceException {
        HashMap params = new HashMap();
        params.put("language", languageCode);
        params.put("detectOrientation", Boolean.valueOf(detectOrientation));
        String path = "https://api.projectoxford.ai/vision/v1.0/ocr";
        String uri = WebServiceRequest.getUrl(path, params);
        params.clear();
        params.put("url", url);
        String json = (String)this.restCall.request(uri, "POST", params, (String)null, false);
        OCR ocr = (OCR)this.gson.fromJson(json, OCR.class);
        return ocr;
    }

    public OCR recognizeText(InputStream stream, String languageCode, boolean detectOrientation) throws VisionServiceException, IOException {
        HashMap params = new HashMap();
        params.put("language", languageCode);
        params.put("detectOrientation", Boolean.valueOf(detectOrientation));
        String path = "https://api.projectoxford.ai/vision/v1.0/ocr";
        String uri = WebServiceRequest.getUrl(path, params);
        byte[] data = IOUtils.toByteArray(stream);
        params.put("data", data);
        String json = (String)this.restCall.request(uri, "POST", params, "application/octet-stream", false);
        OCR ocr = (OCR)this.gson.fromJson(json, OCR.class);
        return ocr;
    }

    public byte[] getThumbnail(int width, int height, boolean smartCropping, String url) throws VisionServiceException, IOException {
        HashMap params = new HashMap();
        params.put("width", Integer.valueOf(width));
        params.put("height", Integer.valueOf(height));
        params.put("smartCropping", Boolean.valueOf(smartCropping));
        String path = "https://api.projectoxford.ai/vision/v1.0/thumbnails";
        String uri = WebServiceRequest.getUrl(path, params);
        params.clear();
        params.put("url", url);
        InputStream is = (InputStream)this.restCall.request(uri, "POST", params, (String)null, true);
        byte[] image = IOUtils.toByteArray(is);
        if(is != null) {
            is.close();
        }

        return image;
    }

    public byte[] getThumbnail(int width, int height, boolean smartCropping, InputStream stream) throws VisionServiceException, IOException {
        HashMap params = new HashMap();
        params.put("width", Integer.valueOf(width));
        params.put("height", Integer.valueOf(height));
        params.put("smartCropping", Boolean.valueOf(smartCropping));
        String path = "https://api.projectoxford.ai/vision/v1.0/thumbnails";
        String uri = WebServiceRequest.getUrl(path, params);
        params.clear();
        byte[] data = IOUtils.toByteArray(stream);
        params.put("data", data);
        InputStream is = (InputStream)this.restCall.request(uri, "POST", params, "application/octet-stream", true);
        byte[] image = IOUtils.toByteArray(is);
        if(is != null) {
            is.close();
        }

        return image;
    }
}
