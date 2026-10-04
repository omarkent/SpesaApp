package com.omar.spesa;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.view.*;
import android.content.*;
import java.util.*;

public class MainActivity extends Activity {
    private FishGameView game;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        immersive();
        game = new FishGameView(this);
        setContentView(game);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    private void immersive() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    static class Fish {
        float x,y,vx,vy,size,phase;
        int body, accent;
        Fish(Random r, int w, int h) {
            size = 42 + r.nextFloat()*38;
            x = size + r.nextFloat()*Math.max(1,w-size*2);
            y = 120 + r.nextFloat()*Math.max(1,h-220);
            float sp = 1.7f + r.nextFloat()*2.8f;
            vx = r.nextBoolean()? sp : -sp;
            vy = (r.nextFloat()-.5f)*1.4f;
            phase = r.nextFloat()*6.28f;
            int[] c={0xFFFFB13B,0xFF4DD6FF,0xFFFF6B8B,0xFFB7F34A,0xFFC98BFF,0xFFFFF06A};
            body=c[r.nextInt(c.length)];
            accent=Color.WHITE;
        }
    }

    static class Particle {
        float x,y,vx,vy,life;
        Particle(float x,float y,float vx,float vy){this.x=x;this.y=y;this.vx=vx;this.vy=vy;life=1f;}
    }

    static class Ripple {
        float x,y,r,a;
        Ripple(float x,float y){this.x=x;this.y=y;r=12;a=1f;}
    }

    static class FishGameView extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
        Random rnd=new Random();
        ArrayList<Fish> fish=new ArrayList<>();
        ArrayList<Particle> particles=new ArrayList<>();
        ArrayList<Ripple> ripples=new ArrayList<>();
        Bitmap dean;
        int score=0;
        long last=System.nanoTime();

        FishGameView(Context c) {
            super(c);
            setBackgroundColor(0xFF063A53);
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));
            stroke.setStyle(Paint.Style.STROKE);
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            dean=BitmapFactory.decodeResource(getResources(), R.drawable.dean_avatar);
        }

        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            fish.clear();
            int n = Math.max(6, Math.min(10, (w*h)/220000));
            for(int i=0;i<n;i++) fish.add(new Fish(rnd,w,h));
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            long now=System.nanoTime();
            float dt=Math.min(.033f,(now-last)/1_000_000_000f);
            last=now;
            drawWater(c);
            updateFish(dt,getWidth(),getHeight());
            for(Fish f:fish) drawFish(c,f);
            updateFx(dt,c);
            drawHud(c);
            postInvalidateOnAnimation();
        }

        void drawWater(Canvas c){
            Paint bg=new Paint();
            bg.setShader(new LinearGradient(0,0,0,getHeight(),0xFF0A6A8A,0xFF012B40,Shader.TileMode.CLAMP));
            c.drawRect(0,0,getWidth(),getHeight(),bg);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x183FE8FF);
            long t=System.currentTimeMillis();
            for(int i=0;i<10;i++){
                float y=(i+1)*getHeight()/11f;
                float x=(float)((Math.sin(t/900.0+i)*.5+.5)*getWidth());
                c.drawOval(x-150,y-18,x+150,y+18,p);
            }
            p.setColor(0x18FFFFFF);
            for(int i=0;i<22;i++){
                float x=(i*97 % Math.max(1,getWidth()));
                float y=(getHeight() - ((t/22 + i*131)%Math.max(1,getHeight())));
                c.drawCircle(x,y,3+(i%5),p);
            }
        }

        void updateFish(float dt,int w,int h){
            float mul=dt*60f;
            for(Fish f:fish){
                f.phase+=.035f*mul;
                f.x+=f.vx*mul;
                f.y+=(f.vy+(float)Math.sin(f.phase)*.35f)*mul;
                f.vx*=0.998f;
                if(Math.abs(f.vx)<1.3f) f.vx=Math.signum(f.vx==0?1:f.vx)*1.3f;
                if(f.x<-f.size*1.5f){f.x=w+f.size;f.y=120+rnd.nextFloat()*Math.max(1,h-220);}
                if(f.x>w+f.size*1.5f){f.x=-f.size;f.y=120+rnd.nextFloat()*Math.max(1,h-220);}
                if(f.y<100){f.y=100;f.vy=Math.abs(f.vy);}
                if(f.y>h-70){f.y=h-70;f.vy=-Math.abs(f.vy);}
            }
        }

        void drawFish(Canvas c,Fish f){
            boolean left=f.vx<0;
            c.save();
            c.translate(f.x,f.y);
            if(left)c.scale(-1,1);
            float s=f.size;
            p.setStyle(Paint.Style.FILL);
            p.setColor(f.body);
            RectF body=new RectF(-s*.72f,-s*.36f,s*.72f,s*.36f);
            c.drawOval(body,p);
            Path tail=new Path();
            tail.moveTo(-s*.67f,0); tail.lineTo(-s*1.18f,-s*.52f); tail.lineTo(-s*1.04f,0); tail.lineTo(-s*1.18f,s*.52f); tail.close();
            c.drawPath(tail,p);
            p.setColor(0x55FFFFFF);
            c.drawOval(-s*.15f,-s*.30f,s*.42f,-s*.12f,p);
            p.setColor(Color.WHITE); c.drawCircle(s*.43f,-s*.08f,s*.09f,p);
            p.setColor(Color.BLACK); c.drawCircle(s*.46f,-s*.08f,s*.045f,p);
            stroke.setStrokeWidth(Math.max(2,s*.035f)); stroke.setColor(0x66000000);
            c.drawOval(body,stroke);
            c.restore();
        }

        void updateFx(float dt,Canvas c){
            float m=dt*60f;
            for(int i=particles.size()-1;i>=0;i--){
                Particle q=particles.get(i); q.x+=q.vx*m; q.y+=q.vy*m; q.vy+=.03f*m; q.life-=.025f*m;
                if(q.life<=0){particles.remove(i);continue;}
                p.setColor(Color.argb((int)(180*q.life),210,250,255));
                c.drawCircle(q.x,q.y,3+5*q.life,p);
            }
            for(int i=ripples.size()-1;i>=0;i--){
                Ripple r=ripples.get(i); r.r+=4*m; r.a-=.035f*m;
                if(r.a<=0){ripples.remove(i);continue;}
                stroke.setStrokeWidth(4); stroke.setColor(Color.argb((int)(180*r.a),230,255,255));
                c.drawCircle(r.x,r.y,r.r,stroke);
            }
        }

        void drawHud(Canvas c){
            float pad=18, av=78;
            p.setColor(0xCC001722); c.drawRoundRect(pad,pad,pad+270,pad+100,34,34,p);
            Path clip=new Path(); clip.addCircle(pad+50,pad+50,38,Path.Direction.CW);
            c.save(); c.clipPath(clip);
            if(dean!=null)c.drawBitmap(dean,null,new RectF(pad+10,pad+10,pad+90,pad+90),p);
            c.restore();
            stroke.setStrokeWidth(3); stroke.setColor(Color.WHITE); c.drawCircle(pad+50,pad+50,39,stroke);
            p.setColor(Color.WHITE); p.setTextSize(23); p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("DEAN",pad+102,pad+40,p);
            p.setColor(0xFFFFD54F); p.setTextSize(19);
            c.drawText("ZAMPATE  "+score,pad+102,pad+72,p);
            p.setColor(0xAAFFFFFF); p.setTextSize(14); p.setTypeface(Typeface.DEFAULT);
            c.drawText("tocca i pesci!",pad+102,pad+93,p);
        }

        void hit(float x,float y){
            ripples.add(new Ripple(x,y));
            Fish best=null; float bd=Float.MAX_VALUE;
            for(Fish f:fish){
                float dx=x-f.x,dy=y-f.y,d=dx*dx+dy*dy;
                float hit=f.size*1.25f;
                if(d<hit*hit && d<bd){best=f;bd=d;}
            }
            if(best!=null){
                score++;
                float dx=best.x-x,dy=best.y-y;
                float len=(float)Math.sqrt(dx*dx+dy*dy)+.01f;
                best.vx=(dx/len)*7.5f;
                best.vy=(dy/len)*4.2f;
                for(int i=0;i<14;i++){
                    float a=(float)(rnd.nextFloat()*Math.PI*2), sp=1+rnd.nextFloat()*3;
                    particles.add(new Particle(x,y,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp));
                }
            }
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN){
                int idx=e.getActionIndex(); hit(e.getX(idx),e.getY(idx)); return true;
            }
            return true;
        }
    }
}
