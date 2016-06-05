package com.example.zane.testshareelement;

import android.annotation.TargetApi;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.List;
import java.util.Map;

/**
 * Created by Zane on 16/6/2.
 * Email: zanebot96@gmail.com
 */

public class EnterActivity extends AppCompatActivity{

    private static final String TAG = EnterActivity.class.getSimpleName();

    private ImageView imageView;
    private EasyDragHelper easyDragHelper;
    private TextView textView;
    private static final String IMAGE_ENTER_NAME = "image_enter";
    private SharedElementCallback mCallBack;
    private String imageName;
    private boolean isReturn;
    private boolean isDisappear;

    private Callback callback = new Callback() {
        @Override
        public void onSuccess() {
           startPost();
        }

        @Override
        public void onError() {
            startPost();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter);
        //因为现在onCreat()是enter状态,所以直接推迟绘制
        postponeEnterTransition();
        initCallBack();

        imageView = (ImageView) findViewById(R.id.image_enter);
        textView = (TextView) findViewById(R.id.text_enter);
        easyDragHelper = (EasyDragHelper) findViewById(R.id.drag_helper);
        imageName = getIntent().getStringExtra(MainActivity.POSITION);

        easyDragHelper.setCallback(new EasyDragHelper.Callback() {
            @Override
            public void onDisappear(int direct) {
                isDisappear = true;
                finish();
            }
        });

        RequestCreator requestCreator;
        if (imageName.equals(MainActivity.IMAGE_NAME_1)){
            requestCreator = Picasso.with(this).load(Constant.URL_1);
            imageView.setTransitionName(MainActivity.IMAGE_NAME_1);
        } else {
            requestCreator = Picasso.with(this).load(Constant.URL_2);
            imageView.setTransitionName(MainActivity.IMAGE_NAME_2);
        }

        requestCreator.into(imageView, callback);
    }


    public void startPost(){
        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startPostponedEnterTransition();
                }
                return true;
            }
        });

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void initCallBack(){
        //防止闪屏
        getWindow().setEnterTransition(null);
        mCallBack = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (isReturn){
                    //如果不这样设置会导致闪烁一下,可能是因为捕捉状态的时候把textview也捕捉进去了,目前还不知道
                    //怎么解决。。。。
                    textView.setAlpha(0f);
                    Log.i(TAG, "return");
                    imageView = getImageView();
                    if (imageView == null){
                        Log.i(TAG, "null");
                        names.clear();
                        sharedElements.clear();
                    }
                } else {
                    Log.i(TAG, "enter");
                }
            }
        };
        setEnterSharedElementCallback(mCallBack);
    }



    /**
     * 在这个activity完成跳转,即将退出(exit)的时候回调这个方法
     */
    @Override
    public void finishAfterTransition() {
        Log.i(TAG, "return");
        isReturn = true;
        Intent intent = new Intent();
        intent.putExtra(MainActivity.POSITION, imageName);
        setResult(RESULT_OK, intent);
        super.finishAfterTransition();
    }

    /**
     * 看这个imageview在不在可视范围里面,如果退出的时候在范围内才会进行组件共享,否则将不会进行组件共享
     * @return
     */
    @Nullable
    private ImageView getImageView(){
        if (isInScreen(getWindow().getDecorView(), imageView)){
            return imageView;
        }
        return null;
    }

    private boolean isInScreen(@NonNull View parent, @NonNull View imageView){
        Rect rect = new Rect();
        //获得decorview的可视矩形
        parent.getHitRect(rect);
        //看imageview在不在这个可视矩形里面
        return imageView.getLocalVisibleRect(rect);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
