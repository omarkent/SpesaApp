package com.omar.spesa;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
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
        float x, y, vx, vy, size, depth;
        float angle, targetAngle, speed, baseSpeed;
        float decision, tail, panic;
        int light, mid, dark, fin;
        boolean striped;

        Fish(Random r, int w, int h) {
            depth = .76f + r.nextFloat() * .28f;
            size = Math.min(w,h) * (.125f + r.nextFloat()*.045f) * depth;
            x = size + r.nextFloat()*Math.max(1f,w-size*2f);
            y = Math.max(150f,size) + r.nextFloat()*Math.max(1f,h-Math.max(300f,size*2f));
            angle = r.nextBoolean()?0f:(float)Math.PI;
            angle += (r.nextFloat()-.5f)*.28f;
            targetAngle = angle;
            baseSpeed = Math.max(2.0f,w*(.00145f+r.nextFloat()*.00062f))*depth;
            speed = baseSpeed;
            vx=(float)Math.cos(angle)*speed;
            vy=(float)Math.sin(angle)*speed;
            decision=1.5f+r.nextFloat()*3.5f;
            tail=r.nextFloat()*6.283f;

            int[][] pal = {
                {0xFFFFF1B8,0xFFE6A02A,0xFF7B4308,0xB5D57B18},
                {0xFFFFD7B5,0xFFE86E3D,0xFF77261A,0xB5B74327},
                {0xFFD6F7FF,0xFF4BAED1,0xFF164E69,0xB52A7893},
                {0xFFE5D7FF,0xFF8B67C6,0xFF3B2C64,0xB55E4798},
                {0xFFE4F7BC,0xFF82B841,0xFF345B24,0xB55D8C31},
                {0xFFF0F4F5,0xFF8B9BA8,0xFF35434E,0xB55D7180}
            };
            int[] c=pal[r.nextInt(pal.length)];
            light=c[0]; mid=c[1]; dark=c[2]; fin=c[3];
            striped=r.nextFloat()<.42f;
        }
    }

    static class Bubble {
        float x,y,r,speed,sway,phase,a;
        Bubble(Random rnd,int w,int h) {
            x=rnd.nextFloat()*w; y=rnd.nextFloat()*h;
            r=2+rnd.nextFloat()*7;
            speed=.45f+rnd.nextFloat()*1.15f;
            sway=6+rnd.nextFloat()*16;
            phase=rnd.nextFloat()*6.283f;
            a=.13f+rnd.nextFloat()*.2f;
        }
    }

    static class Splash {
        float x,y,vx,vy,life,r;
        Splash(float x,float y,float vx,float vy,float r){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.r=r;life=1f;
        }
    }

    static class Ripple {
        float x,y,r,a;
        Ripple(float x,float y){this.x=x;this.y=y;r=12;a=1f;}
    }

    static class FishGameView extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd=new Random();
        final ArrayList<Fish> fish=new ArrayList<>();
        final ArrayList<Bubble> bubbles=new ArrayList<>();
        final ArrayList<Splash> splashes=new ArrayList<>();
        final ArrayList<Ripple> ripples=new ArrayList<>();

        final RectF[] aquariumCards={new RectF(),new RectF(),new RectF(),new RectF()};
        final RectF[] countButtons={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
        final RectF playButton=new RectF();

        Bitmap dean;
        SharedPreferences prefs;
        boolean menu=true;
        int aquarium=0;
        int fishCount=3;
        int score=0;
        float time=0f;
        long last=System.nanoTime();
        long cornerDown=0;
        boolean cornerPressed=false;

        final String[] aquariumNames={"LAGUNA","BARRIERA","OCEANO","SABBIA"};

        FishGameView(Context c){
            super(c);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            dean=BitmapFactory.decodeResource(getResources(),R.drawable.dean_avatar);
            prefs=c.getSharedPreferences("dean_fish_v2",Context.MODE_PRIVATE);
            aquarium=prefs.getInt("aquarium",0);
            fishCount=Math.max(1,Math.min(5,prefs.getInt("fishCount",3)));
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
            buildMenuRects(w,h);
            resetScene();
        }

        void buildMenuRects(int w,int h){
            float margin=w*.06f;
            float gap=w*.025f;
            float cardW=(w-margin*2-gap)/2f;
            float cardH=Math.min(h*.13f,cardW*.62f);
            float startY=h*.22f;
            for(int i=0;i<4;i++){
                int row=i/2,col=i%2;
                float l=margin+col*(cardW+gap);
                float t=startY+row*(cardH+gap);
                aquariumCards[i].set(l,t,l+cardW,t+cardH);
            }

            float btnGap=w*.018f;
            float btnW=(w-margin*2-btnGap*4)/5f;
            float cy=startY+2*(cardH+gap)+h*.085f;
            for(int i=0;i<5;i++){
                float l=margin+i*(btnW+btnGap);
                countButtons[i].set(l,cy,l+btnW,cy+btnW);
            }

            float py=cy+btnW+h*.085f;
            playButton.set(margin,py,w-margin,py+Math.min(h*.08f,110f));
        }

        void resetScene(){
            fish.clear(); bubbles.clear(); splashes.clear(); ripples.clear();
            if(getWidth()<=0||getHeight()<=0)return;
            for(int i=0;i<fishCount;i++) fish.add(new Fish(rnd,getWidth(),getHeight()));
            for(int i=0;i<38;i++) bubbles.add(new Bubble(rnd,getWidth(),getHeight()));
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            long now=System.nanoTime();
            float dt=Math.min(.034f,(now-last)/1_000_000_000f);
            last=now;
            time+=dt;

            drawAquarium(c,dt);

            if(menu){
                drawMenu(c);
            }else{
                updateFish(dt);
                ArrayList<Fish> ordered=new ArrayList<>(fish);
                Collections.sort(ordered,new Comparator<Fish>(){
                    @Override public int compare(Fish a,Fish b){return Float.compare(a.depth,b.depth);}
                });
                for(Fish f:ordered) drawFish(c,f);
                updateFx(dt,c);
                drawHud(c);
            }
            postInvalidateOnAnimation();
        }

        void drawAquarium(Canvas c,float dt){
            int w=getWidth(),h=getHeight();
            int top,mid,bottom;
            switch(aquarium){
                case 1: top=0xFF1595A8;mid=0xFF0A5975;bottom=0xFF073348;break;
                case 2: top=0xFF0B5875;mid=0xFF06354F;bottom=0xFF011B30;break;
                case 3: top=0xFF2BA2AE;mid=0xFF157484;bottom=0xFF0B5365;break;
                default: top=0xFF2BB7C3;mid=0xFF138298;bottom=0xFF07536C;
            }
            Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setShader(new LinearGradient(0,0,0,h,new int[]{top,mid,bottom},new float[]{0,.48f,1},Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,bg);

            drawLightRays(c);
            drawCaustics(c);

            if(aquarium==0) drawLagoonBottom(c);
            else if(aquarium==1) drawReef(c);
            else if(aquarium==2) drawDeep(c);
            else drawSand(c);

            drawBubbles(c,dt);
            drawVignette(c);
        }

        void drawLightRays(Canvas c){
            int w=getWidth(),h=getHeight();
            Paint ray=new Paint(Paint.ANTI_ALIAS_FLAG);
            ray.setShader(new LinearGradient(0,0,0,h*.78f,0x31E6FFFF,0x00E6FFFF,Shader.TileMode.CLAMP));

            for(int i=0;i<3;i++){
                float sx=w*(.08f+i*.30f)+(float)Math.sin(time*.14f+i)*w*.03f;
                float sw=w*(.11f+i*.025f);
                Path path=new Path();
                path.moveTo(sx,0);path.lineTo(sx+sw,0);
                path.lineTo(sx+sw*2.0f,h*.77f);path.lineTo(sx-sw*.55f,h*.77f);
                path.close();
                c.drawPath(path,ray);
            }
            ray.setShader(null);
        }

        void drawCaustics(Canvas c){
            int w=getWidth(),h=getHeight();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            for(int row=0;row<9;row++){
                float yy=h*(.055f+row*.072f);
                p.setStrokeWidth(3.5f+row*.32f);
                p.setColor(Color.argb(Math.max(8,28-row*2),220,255,255));
                Path line=new Path();
                for(int i=0;i<=22;i++){
                    float xx=i*w/22f;
                    float wave=(float)Math.sin(i*.85f+time*.9f+row*1.31f)*(6+row*1.3f);
                    float drift=(float)Math.sin(time*.27f+row)*18f;
                    if(i==0)line.moveTo(xx+drift,yy+wave);else line.lineTo(xx+drift,yy+wave);
                }
                c.drawPath(line,p);
            }
            p.setStyle(Paint.Style.FILL);
        }

        void drawLagoonBottom(Canvas c){
            int w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,h*.80f,0,h,0x00D8E6B5,0x8FD6D6A0,Shader.TileMode.CLAMP));
            c.drawRect(0,h*.77f,w,h,p);p.setShader(null);

            for(int i=0;i<13;i++){
                float x=(i*.087f%1f)*w;
                float base=h-(i%4)*9;
                float height=h*(.055f+(i%5)*.012f);
                stroke.setStrokeWidth(4+(i%3));
                stroke.setColor(0x88498D62);
                Path g=new Path();g.moveTo(x,base);
                g.quadTo(x+(float)Math.sin(time+i)*12,base-height*.55f,x+(float)Math.sin(time*.7f+i)*20,base-height);
                c.drawPath(g,stroke);
            }
        }

        void drawReef(Canvas c){
            int w=getWidth(),h=getHeight();
            p.setShader(null);
            p.setColor(0x7A082C36);
            c.drawRect(0,h*.87f,w,h,p);
            int[] coralColors={0xAAE05F55,0xAACD7A37,0xAA6B8F70,0xAA9B5DA6};
            for(int i=0;i<10;i++){
                float x=(i+.3f)*w/10f;
                float base=h*.91f+(i%3)*13;
                float ht=h*(.045f+(i%4)*.025f);
                p.setColor(coralColors[i%coralColors.length]);
                float r=12+(i%3)*4;
                c.drawRoundRect(x-r,base-ht,x+r,base,r,r,p);
                c.drawCircle(x-r*.8f,base-ht*.62f,r*.7f,p);
                c.drawCircle(x+r*.9f,base-ht*.43f,r*.75f,p);
            }
        }

        void drawDeep(Canvas c){
            int w=getWidth(),h=getHeight();
            p.setShader(null);
            for(int i=0;i<12;i++){
                float x=(i*137%Math.max(1,w));
                float y=h*.58f+(i%5)*h*.07f;
                p.setColor(Color.argb(18+(i%3)*7,110,175,192));
                c.drawCircle(x,y,2+(i%4),p);
            }
        }

        void drawSand(Canvas c){
            int w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,h*.76f,0,h,0x00E7D3A5,0xC8D4B37C,Shader.TileMode.CLAMP));
            Path dune=new Path();
            dune.moveTo(0,h);
            dune.lineTo(0,h*.86f);
            for(int i=0;i<=8;i++){
                float x=i*w/8f;
                float y=h*(.86f+(float)Math.sin(i*.9f)*.014f);
                dune.lineTo(x,y);
            }
            dune.lineTo(w,h);dune.close();
            c.drawPath(dune,p);p.setShader(null);
            for(int i=0;i<16;i++){
                float x=(i*.071f%1f)*w;
                float y=h*(.89f+(i%5)*.017f);
                p.setColor(i%2==0?0x664C6264:0x667B7462);
                c.drawOval(x-10,y-5,x+10+(i%4)*5,y+5+(i%3)*3,p);
            }
        }

        void drawBubbles(Canvas c,float dt){
            int w=getWidth(),h=getHeight();
            for(Bubble b:bubbles){
                b.phase+=dt*.9f;
                b.y-=b.speed*dt*60f;
                b.x+=(float)Math.sin(b.phase)*b.sway*dt*.11f*60f;
                if(b.y<-15){b.y=h+15;b.x=rnd.nextFloat()*w;}
                int a=(int)(255*b.a);
                p.setShader(null);
                p.setColor(Color.argb(a,225,251,255));
                c.drawCircle(b.x,b.y,b.r,p);
                p.setColor(Color.argb(Math.min(180,a+45),255,255,255));
                c.drawCircle(b.x-b.r*.28f,b.y-b.r*.28f,Math.max(1,b.r*.18f),p);
            }
        }

        void drawVignette(Canvas c){
            int w=getWidth(),h=getHeight();
            Paint v=new Paint(Paint.ANTI_ALIAS_FLAG);
            v.setShader(new RadialGradient(w*.5f,h*.44f,Math.max(w,h)*.72f,0x00000000,0x52000C1B,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,v);
        }

        void updateFish(float dt){
            int w=getWidth(),h=getHeight();
            float frame=dt*60f;
            for(Fish f:fish){
                f.decision-=dt;
                f.tail+=dt*(f.panic>0?15f:7f+f.speed*.22f);

                if(f.panic>0){
                    f.panic-=dt;
                }else if(f.decision<=0){
                    f.decision=1.6f+rnd.nextFloat()*3.6f;
                    f.targetAngle+=(rnd.nextFloat()-.5f)*.85f;
                    f.baseSpeed=Math.max(1.8f,w*(.00135f+rnd.nextFloat()*.00062f))*f.depth;
                }

                float margin=f.size*1.05f;
                float ax=0,ay=0;
                if(f.x<margin)ax+=(margin-f.x)/margin;
                if(f.x>w-margin)ax-=(f.x-(w-margin))/margin;
                float top=Math.max(135f,margin*.78f);
                float bottom=h-margin*.72f;
                if(f.y<top)ay+=(top-f.y)/top;
                if(f.y>bottom)ay-=(f.y-bottom)/margin;

                if(Math.abs(ax)>0.001f||Math.abs(ay)>0.001f){
                    f.targetAngle=(float)Math.atan2(ay*1.35f,ax*1.7f);
                }

                float turnRate=(f.panic>0?.15f:.035f)*frame;
                float delta=wrapAngle(f.targetAngle-f.angle);
                f.angle+=delta*Math.min(1f,turnRate);

                float targetSpeed=f.baseSpeed*(f.panic>0?3.6f:1f);
                f.speed+=(targetSpeed-f.speed)*Math.min(1f,(f.panic>0?.18f:.035f)*frame);

                float desiredVx=(float)Math.cos(f.angle)*f.speed;
                float desiredVy=(float)Math.sin(f.angle)*f.speed;
                f.vx+=(desiredVx-f.vx)*Math.min(1f,.11f*frame);
                f.vy+=(desiredVy-f.vy)*Math.min(1f,.11f*frame);

                f.x+=f.vx*frame;
                f.y+=f.vy*frame + (float)Math.sin(time*1.7f+f.tail*.12f)*.16f*frame;

                if(f.x<4){f.x=4;f.targetAngle=0;}
                if(f.x>w-4){f.x=w-4;f.targetAngle=(float)Math.PI;}
                if(f.y<95){f.y=95;f.targetAngle=Math.abs(f.angle);}
                if(f.y>h-38){f.y=h-38;f.targetAngle=-Math.abs(f.angle);}
            }
        }

        float wrapAngle(float a){
            while(a>(float)Math.PI)a-=(float)Math.PI*2f;
            while(a<-(float)Math.PI)a+=(float)Math.PI*2f;
            return a;
        }

        void drawFish(Canvas c,Fish f){
            float s=f.size;
            c.save();
            c.translate(f.x,f.y);
            c.rotate((float)Math.toDegrees(f.angle));

            float tailSwing=(float)Math.sin(f.tail)*s*(f.panic>0?.15f:.075f);

            // depth shadow
            p.setShader(null);p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb((int)(38*f.depth),0,10,18));
            c.drawOval(-s*.86f,s*.31f,s*.78f,s*.51f,p);

            // tail
            Path tail=new Path();
            tail.moveTo(-s*.55f,0);
            tail.cubicTo(-s*.83f,-s*.16f,-s*.95f,-s*.55f,-s*1.08f,-s*.46f+tailSwing*.18f);
            tail.quadTo(-s*.93f,tailSwing,-s*1.08f,s*.46f+tailSwing*.18f);
            tail.cubicTo(-s*.94f,s*.55f,-s*.82f,s*.15f,-s*.55f,0);
            tail.close();
            p.setShader(new LinearGradient(-s*1.08f,0,-s*.48f,0,f.dark,f.mid,Shader.TileMode.CLAMP));
            c.drawPath(tail,p);

            // dorsal fin
            Path dorsal=new Path();
            dorsal.moveTo(-s*.28f,-s*.25f);
            dorsal.quadTo(-s*.03f,-s*.64f,s*.29f,-s*.27f);
            dorsal.close();
            p.setShader(new LinearGradient(0,-s*.64f,0,-s*.18f,f.mid,f.dark,Shader.TileMode.CLAMP));
            c.drawPath(dorsal,p);

            // body silhouette with pointed head
            Path body=new Path();
            body.moveTo(-s*.62f,0);
            body.cubicTo(-s*.46f,-s*.36f,s*.16f,-s*.41f,s*.58f,-s*.22f);
            body.quadTo(s*.78f,0,s*.58f,s*.22f);
            body.cubicTo(s*.16f,s*.41f,-s*.46f,s*.36f,-s*.62f,0);
            body.close();

            p.setShader(new LinearGradient(0,-s*.38f,0,s*.38f,
                new int[]{f.light,f.mid,f.dark},
                new float[]{0,.46f,1},Shader.TileMode.CLAMP));
            c.drawPath(body,p);

            // specular highlight
            p.setShader(new RadialGradient(s*.18f,-s*.13f,s*.65f,
                new int[]{0xB9FFFFFF,0x31FFFFFF,0x00FFFFFF},
                new float[]{0,.38f,1},Shader.TileMode.CLAMP));
            c.drawPath(body,p);

            // side depth shading
            p.setShader(new LinearGradient(-s*.6f,0,s*.7f,0,0x29001826,0x00001826,Shader.TileMode.CLAMP));
            c.drawPath(body,p);

            // gill shadow
            stroke.setShader(null);stroke.setStrokeWidth(Math.max(2f,s*.018f));
            stroke.setColor(0x69411C16);
            c.drawArc(new RectF(s*.30f,-s*.21f,s*.53f,s*.22f),104,150,false,stroke);

            // scales / bands
            p.setShader(null);
            if(f.striped){
                stroke.setStrokeWidth(Math.max(2f,s*.02f));
                stroke.setColor(0x3EFFFFFF);
                for(int i=-2;i<=2;i++){
                    float x=i*s*.16f;
                    c.drawArc(new RectF(x-s*.13f,-s*.28f,x+s*.13f,s*.28f),68,224,false,stroke);
                }
            }else{
                p.setColor(0x20FFFFFF);
                for(int row=0;row<3;row++)for(int col=0;col<5;col++){
                    float x=-s*.32f+col*s*.16f+(row%2)*s*.07f;
                    float y=-s*.13f+row*s*.12f;
                    c.drawCircle(x,y,s*.023f,p);
                }
            }

            // pectoral fin
            Path fin=new Path();
            fin.moveTo(s*.08f,s*.11f);
            fin.quadTo(-s*.02f,s*.49f,s*.33f,s*.29f);
            fin.quadTo(s*.28f,s*.13f,s*.08f,s*.11f);
            fin.close();
            p.setColor(f.fin);
            c.drawPath(fin,p);

            // eye
            p.setColor(0x4D000000);c.drawCircle(s*.46f,-s*.105f,s*.10f,p);
            p.setColor(0xFFF4F2DE);c.drawCircle(s*.46f,-s*.11f,s*.078f,p);
            p.setColor(0xFF0D1114);c.drawCircle(s*.483f,-s*.11f,s*.043f,p);
            p.setColor(Color.WHITE);c.drawCircle(s*.50f,-s*.132f,s*.014f,p);

            // mouth
            stroke.setStrokeWidth(Math.max(2f,s*.014f));stroke.setColor(0x8D54251D);
            c.drawArc(new RectF(s*.54f,-s*.015f,s*.72f,s*.13f),26,92,false,stroke);

            // rim light
            stroke.setStrokeWidth(Math.max(1.5f,s*.013f));
            stroke.setColor(0x65E8FFFF);
            c.drawArc(new RectF(-s*.56f,-s*.31f,s*.65f,s*.31f),198,145,false,stroke);

            c.restore();
        }

        void updateFx(float dt,Canvas c){
            float frame=dt*60f;
            for(int i=splashes.size()-1;i>=0;i--){
                Splash q=splashes.get(i);
                q.x+=q.vx*frame;q.y+=q.vy*frame;q.vy+=.035f*frame;q.life-=.025f*frame;
                if(q.life<=0){splashes.remove(i);continue;}
                p.setShader(null);
                p.setColor(Color.argb((int)(180*q.life),220,252,255));
                c.drawCircle(q.x,q.y,q.r*(.5f+.5f*q.life),p);
            }
            for(int i=ripples.size()-1;i>=0;i--){
                Ripple r=ripples.get(i);r.r+=5f*frame;r.a-=.032f*frame;
                if(r.a<=0){ripples.remove(i);continue;}
                stroke.setStrokeWidth(4);stroke.setColor(Color.argb((int)(175*r.a),230,255,255));
                c.drawCircle(r.x,r.y,r.r,stroke);
                stroke.setStrokeWidth(2);stroke.setColor(Color.argb((int)(95*r.a),230,255,255));
                c.drawCircle(r.x,r.y,r.r*.63f,stroke);
            }
        }

        void drawHud(Canvas c){
            float pad=16f;
            p.setShader(null);p.setColor(0xB8001723);
            c.drawRoundRect(pad,pad,pad+250,pad+88,30,30,p);

            Path clip=new Path();clip.addCircle(pad+43,pad+44,34,Path.Direction.CW);
            c.save();c.clipPath(clip);
            if(dean!=null)c.drawBitmap(dean,null,new RectF(pad+9,pad+10,pad+77,pad+78),p);
            c.restore();

            stroke.setStrokeWidth(2.5f);stroke.setColor(0xEFFFFFFF);
            c.drawCircle(pad+43,pad+44,35,stroke);

            p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(22);p.setColor(Color.WHITE);
            c.drawText("DEAN",pad+92,pad+35,p);
            p.setTextSize(18);p.setColor(0xFFFFD75A);
            c.drawText("ZAMPATE  "+score,pad+92,pad+62,p);

            p.setTypeface(Typeface.DEFAULT);p.setTextSize(11);p.setColor(0x99FFFFFF);
            c.drawText("tieni premuto in alto a destra per menu",pad+92,pad+79,p);
        }

        void drawMenu(Canvas c){
            int w=getWidth(),h=getHeight();

            p.setShader(null);p.setColor(0xB000101B);
            c.drawRoundRect(w*.035f,h*.055f,w*.965f,h*.94f,42,42,p);

            // Dean portrait
            float avatar=Math.min(w*.19f,150f);
            float ax=w*.5f-avatar*.5f, ay=h*.075f;
            Path clip=new Path();clip.addCircle(w*.5f,ay+avatar*.5f,avatar*.5f,Path.Direction.CW);
            c.save();c.clipPath(clip);
            if(dean!=null)c.drawBitmap(dean,null,new RectF(ax,ay,ax+avatar,ay+avatar),p);
            c.restore();
            stroke.setStrokeWidth(4);stroke.setColor(Color.WHITE);
            c.drawCircle(w*.5f,ay+avatar*.5f,avatar*.5f+2,stroke);

            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(Math.min(42f,w*.075f));p.setColor(Color.WHITE);
            c.drawText("DEAN FISH HUNT",w*.5f,h*.18f,p);

            p.setTextSize(Math.min(21f,w*.043f));p.setColor(0xFFBEEFFF);
            c.drawText("SCEGLI L'ACQUARIO",w*.5f,h*.215f,p);

            for(int i=0;i<4;i++){
                RectF r=aquariumCards[i];
                boolean sel=i==aquarium;
                p.setColor(sel?0xE625AFC5:0xA51A4657);
                c.drawRoundRect(r,26,26,p);
                stroke.setStrokeWidth(sel?5:2);
                stroke.setColor(sel?0xFFFFFFFF:0x66D9F8FF);
                c.drawRoundRect(r,26,26,stroke);

                float cx=r.centerX(),cy=r.centerY()-8;
                drawAquariumIcon(c,i,cx,cy,Math.min(r.width(),r.height())*.24f);
                p.setTextSize(Math.min(18f,w*.034f));
                p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setColor(Color.WHITE);
                c.drawText(aquariumNames[i],cx,r.bottom-14,p);
            }

            float labelY=countButtons[0].top-h*.035f;
            p.setTextSize(Math.min(21f,w*.043f));p.setColor(0xFFBEEFFF);
            c.drawText("NUMERO DI PESCI",w*.5f,labelY,p);

            for(int i=0;i<5;i++){
                RectF r=countButtons[i];
                boolean sel=(i+1)==fishCount;
                p.setColor(sel?0xE6FFD45A:0xA51A4657);
                c.drawOval(r,p);
                stroke.setStrokeWidth(sel?5:2);stroke.setColor(sel?0xFFFFFFFF:0x66D9F8FF);
                c.drawOval(r,stroke);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextSize(Math.min(32f,r.height()*.42f));
                p.setColor(sel?0xFF13202A:Color.WHITE);
                c.drawText(""+(i+1),r.centerX(),r.centerY()+p.getTextSize()*.34f,p);
            }

            p.setColor(0xE6FFCF45);
            c.drawRoundRect(playButton,32,32,p);
            stroke.setStrokeWidth(4);stroke.setColor(Color.WHITE);c.drawRoundRect(playButton,32,32,stroke);
            p.setColor(0xFF10202A);p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(Math.min(30f,w*.058f));
            c.drawText("GIOCA",playButton.centerX(),playButton.centerY()+p.getTextSize()*.34f,p);

            p.setTypeface(Typeface.DEFAULT);p.setTextSize(Math.min(15f,w*.029f));p.setColor(0xAFFFFFFF);
            c.drawText("V2 • pesci grandi • movimento naturale • acqua dinamica",w*.5f,playButton.bottom+h*.035f,p);
            p.setTextAlign(Paint.Align.LEFT);
        }

        void drawAquariumIcon(Canvas c,int type,float x,float y,float s){
            p.setShader(null);
            if(type==0){
                p.setColor(0xFF6AD9DF);c.drawCircle(x,y,s,p);
                p.setColor(0xFFB6DA9A);c.drawOval(x-s*.8f,y+s*.2f,x+s*.8f,y+s*.75f,p);
            }else if(type==1){
                p.setColor(0xFF3495B0);c.drawCircle(x,y,s,p);
                p.setColor(0xFFE5785F);c.drawRoundRect(x-s*.14f,y,x+s*.14f,y+s*.85f,8,8,p);
                c.drawCircle(x-s*.27f,y+s*.3f,s*.26f,p);c.drawCircle(x+s*.28f,y+s*.12f,s*.23f,p);
            }else if(type==2){
                p.setColor(0xFF174965);c.drawCircle(x,y,s,p);
                p.setColor(0x55FFFFFF);c.drawCircle(x-s*.22f,y-s*.25f,s*.08f,p);
                c.drawCircle(x+s*.32f,y+s*.1f,s*.05f,p);
            }else{
                p.setColor(0xFF4DB4BE);c.drawCircle(x,y,s,p);
                p.setColor(0xFFD2B87F);c.drawOval(x-s*.85f,y+s*.28f,x+s*.85f,y+s*.82f,p);
            }
        }

        void hitFish(float x,float y){
            ripples.add(new Ripple(x,y));
            Fish best=null;float bestD=Float.MAX_VALUE;
            for(Fish f:fish){
                float dx=x-f.x,dy=y-f.y;
                float d=dx*dx+dy*dy;
                float hit=f.size*1.25f;
                if(d<hit*hit&&d<bestD){best=f;bestD=d;}
            }
            if(best!=null){
                score++;
                float dx=best.x-x,dy=best.y-y;
                float len=(float)Math.sqrt(dx*dx+dy*dy);
                if(len<1f){dx=(rnd.nextFloat()-.5f);dy=(rnd.nextFloat()-.5f);len=(float)Math.sqrt(dx*dx+dy*dy)+.01f;}
                best.targetAngle=(float)Math.atan2(dy/len,dx/len);
                best.panic=.72f;
                best.speed=best.baseSpeed*3.7f;
                for(int i=0;i<18;i++){
                    float a=rnd.nextFloat()*6.283f;
                    float sp=1f+rnd.nextFloat()*3.6f;
                    splashes.add(new Splash(x,y,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,3+rnd.nextFloat()*5));
                }
            }
        }

        void startGame(){
            prefs.edit().putInt("aquarium",aquarium).putInt("fishCount",fishCount).apply();
            score=0;menu=false;last=System.nanoTime();resetScene();
        }

        void openMenu(){
            menu=true;cornerPressed=false;buildMenuRects(getWidth(),getHeight());
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked();
            float x=e.getX(e.getActionIndex()), y=e.getY(e.getActionIndex());

            if(menu){
                if(action==MotionEvent.ACTION_DOWN){
                    for(int i=0;i<4;i++){
                        if(aquariumCards[i].contains(x,y)){aquarium=i;invalidate();return true;}
                    }
                    for(int i=0;i<5;i++){
                        if(countButtons[i].contains(x,y)){fishCount=i+1;invalidate();return true;}
                    }
                    if(playButton.contains(x,y)){startGame();return true;}
                }
                return true;
            }

            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                if(x>getWidth()*.82f&&y<getHeight()*.16f&&action==MotionEvent.ACTION_DOWN){
                    cornerPressed=true;cornerDown=System.currentTimeMillis();
                }else{
                    hitFish(x,y);
                }
                return true;
            }

            if(action==MotionEvent.ACTION_UP){
                if(cornerPressed){
                    long held=System.currentTimeMillis()-cornerDown;
                    cornerPressed=false;
                    if(held>=1800){openMenu();return true;}
                    hitFish(x,y);
                }
                return true;
            }

            if(action==MotionEvent.ACTION_MOVE&&cornerPressed){
                if(x<getWidth()*.76f||y>getHeight()*.22f)cornerPressed=false;
            }
            return true;
        }
    }
}
