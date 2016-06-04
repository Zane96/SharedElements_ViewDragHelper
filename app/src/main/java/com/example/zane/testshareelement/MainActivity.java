package com.example.zane.testshareelement;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    /**
     * 共享组件大概原理:首先指定组件,然后捕捉组件在exiting/enter和return/reenter中的结束/开始的状态
     * 这里的状态包括size ,layout等,然后自动生成动画,并且在view渲染的最高层级(ViewOverlay)绘制出来这个组件,所以有组件被
     * 共享的效果
     * 如果你的图片是异步加载,那么就需要通过postxxxxx()方法推迟view的绘制,因为我必须要等到图片加载出来了之后
     * 才能得到图片的大小等信息,给要共享的组件添加addPreDrawListener监听回调,在图片准备开始绘制的时候startPostxxxx()
     *
     * getWindow.setExitTransition(null)
     * getWindow.setEnterTransition(null)可以防止activity的fade out和fade in的动画效果,防止闪屏
     * 具体问题讨论:http://stackoverflow.com/questions/28364106/blinking-screen-on-image-transition-between-activities
     */
    private ImageView imageView_1;
    private ImageView imageView_2;
    private SharedElementCallback mCallback;
    //记录是点击哪一个imageview进入
    public static final String POSITION = "POSITION";
    public static final String IMAGE_NAME_1 = "image_1";
    public static final String IMAGE_NAME_2 = "image_2";
    private static final String TAG = "MainActivity2";
    /**
     * 两个转换的activity可以通过bundle来传递数据,并且还可以在回调里面判断现在
     * 是exit状态还是reenter状态,其实不回传数据的话,这个也没什么必要。
     * 所以不传递数据的话,用boolean就可以了
     */
    private boolean isReenterState = false;
    private String imageTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initCallBack();

        imageView_1 = (ImageView) findViewById(R.id.imageview_1);
        imageView_2 = (ImageView) findViewById(R.id.iamgeview_2);
        Picasso.with(MainActivity.this).load(Constant.URL_1).into(imageView_1);
        Picasso.with(MainActivity.this).load(Constant.URL_2).into(imageView_2);
        imageView_1.setOnClickListener(this);
        imageView_2.setOnClickListener(this);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void initCallBack(){
        //监听reenter的状态
        mCallback = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                /**
                 * 如果是reenter状态,不用做任何事情,
                 * 因为系统会默认return/reenter的element就是exiting/enter中共享的
                 * 如果reenter的时候,需要共享的组件变了,那么就需要重新设置sharedElements了
                 */
                if (isReenterState){
                    Log.i(TAG, "reenter");
                    isReenterState = false;
                } else {
                    //如果是exiting状态,除了后面要共享的ImageView,这里添加状态栏和导航栏
                    Log.i(TAG, "exiting");
                    //防止闪屏
                    getWindow().setExitTransition(null);
                    View statusBar = findViewById(android.R.id.statusBarBackground);
                    View navigationBar = findViewById(android.R.id.navigationBarBackground);
                    if (statusBar != null){
                        //应该系统控件默认set了transName的吧。。。。
                        names.add(statusBar.getTransitionName());
                        sharedElements.put(statusBar.getTransitionName(), statusBar);
                    }
                    if (navigationBar != null){
                        names.add(navigationBar.getTransitionName());
                        sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                    }
                }
            }
        };
        setExitSharedElementCallback(mCallback);
    }

    /**
     * 在监听Reenter的过程中,首先要推迟共享组件的绘制,所以监听view的状态,
     * 一旦view的size,layout确定了之后就赶紧开始绘制共享组件因为共享组件需要在顶层级绘制
     * @param resultCode
     * @param data
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        //推迟绘制
        postponeEnterTransition();

        isReenterState = true;
        String imageName = data.getStringExtra(POSITION);
        if (imageName.equals(IMAGE_NAME_1)){
            imageView_1.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    Toast.makeText(MainActivity.this, "reenter", Toast.LENGTH_SHORT).show();
                    //gg,这里需要匿名内部类的引用,还用不了lambda
                    imageView_1.getViewTreeObserver().removeOnPreDrawListener(this);
                    startPostponedEnterTransition();
                    return true;
                }
            });
        } else {
            imageView_2.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    imageView_2.getViewTreeObserver().removeOnPreDrawListener(this);
                    startPostponedEnterTransition();
                    return true;
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, EnterActivity.class);
        //为了兼容5.0以下的跳转,还是要把是由那个imageview点击启动的信息传给后面的页面
        switch(v.getId()){
            case R.id.imageview_1:
                imageTag = IMAGE_NAME_1;
                intent.putExtra(POSITION, IMAGE_NAME_1);

                    imageView_1.setTransitionName(IMAGE_NAME_1);
                    Log.i(TAG, "start");
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(MainActivity.this
                            , imageView_1, imageView_1.getTransitionName()).toBundle());

                break;

            case R.id.iamgeview_2:
                imageTag = IMAGE_NAME_2;
                intent.putExtra(POSITION, IMAGE_NAME_2);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    Log.i(TAG, IMAGE_NAME_2);
                    imageView_2.setTransitionName(IMAGE_NAME_2);
                    Log.i(TAG, imageView_2.getTransitionName() + " trans");
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(MainActivity.this
                            , imageView_2, imageView_2.getTransitionName()).toBundle());
                }
                break;
            default:
        }
    }
}
