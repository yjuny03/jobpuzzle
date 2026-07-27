//  puzzle-scene.js 역할: HERO 전체 배경의 WebGL 3D 퍼즐 생성, 조명, 조립 단계, 드래그 회전을 담당합니다

(function () {
    'use strict';

    //  01. WebGL 대상 canvas와 렌더링 컨텍스트 준비: 미지원 시 canvas를 숨기고 종료
    var canvas = document.getElementById('jobPuzzleCanvas');
    if (!canvas) return;
    var gl = canvas.getContext('webgl', { alpha:true, antialias:true, premultipliedAlpha:false });
    if (!gl) { canvas.hidden = true; return; }

    //  02. 퍼즐 클릭 단계별 UI 데이터: 하단 단계 문구와 우측 홀로그램 화면을 함께 변경
    var stages = [
        { label:'자료 확정', title:'등록 자료 확정', pill:'추출 확인', note:'AI 추출 결과를 직접 확인하고 수정한 확정본만 분석에 사용합니다.' },
        { label:'공고·지원자 분석', title:'공고·경험 분석', pill:'분석 완료', note:'공고·회사정보와 지원자 경험을 각각 분석해 면접 근거를 정리합니다.' },
        { label:'연결도·적합도 진단', title:'요구사항 연결 진단', pill:'적합도 산출', note:'요구사항별 연결도와 부족한 근거를 진단하고 종합 적합도를 보여줍니다.' },
        { label:'질문·과제 생성', title:'맞춤 질문·과제', pill:'사전 생성', note:'연결도에 따라 맞춤 질문과 보완 과제를 면접 전에 미리 생성합니다.' },
        { label:'면접·답변 평가', title:'답변 관점 평가', pill:'8개 관점', note:'답변을 8개 고정 관점으로 평가하고 반복되는 약점 태그를 연결합니다.' },
        { label:'약점 보완·리포트', title:'다음 연습 리포트', pill:'준비 완료', note:'약점 보완 방향과 자료 수정 제안, 다음 연습 과정을 하나의 리포트로 제공합니다.' }
    ];

    //  03. 퍼즐 단계와 연동할 HTML 요소 참조
    var label = document.getElementById('puzzle-stage-label');
    var bar = document.getElementById('puzzle-stage-progress');
    var hologram = document.querySelector('.hero-hologram');
    var hologramPanels = hologram ? Array.prototype.slice.call(hologram.querySelectorAll('.hologram-panel')) : [];
    var hologramDots = hologram ? Array.prototype.slice.call(hologram.querySelectorAll('.hologram-step-dot')) : [];
    var hologramNumber = document.getElementById('hologram-stage-number');

    //  04. GLSL 셰이더 소스: vertex는 위치/법선 변환, fragment는 ambient·key/fill light·specular·rim light 계산
    var vertex = 'attribute vec3 p;attribute vec3 n;uniform mat4 m;uniform mat4 pv;varying vec3 N;varying vec3 W;void main(){vec4 w=m*vec4(p,1.);W=w.xyz;N=normalize(mat3(m)*n);gl_Position=pv*w;}';
    var fragment = 'precision mediump float;uniform vec3 c;varying vec3 N;varying vec3 W;void main(){vec3 nn=normalize(N);vec3 key=normalize(vec3(-.35,.82,.68));vec3 fill=normalize(vec3(.48,.30,-.72));vec3 v=normalize(vec3(0.,0.,7.)-W);vec3 h=normalize(key+v);float diff=max(dot(nn,key),0.);float fillDiff=max(dot(nn,fill),0.);float spec=pow(max(dot(nn,h),0.),34.);float rim=pow(1.-max(dot(nn,v),0.),2.1);vec3 base=c*(.48+diff*.42+fillDiff*.18);vec3 sheen=vec3(.72,.9,1.)*(spec*.34+rim*.16);gl_FragColor=vec4(min(base+sheen,vec3(1.)),1.);}';
    //  개별 셰이더를 생성하고 소스 컴파일
    function shader(type, source) { var s=gl.createShader(type); gl.shaderSource(s,source); gl.compileShader(s); return s; }
    //  vertex/fragment 셰이더를 WebGL program에 연결하고 사용 상태로 전환
    var program=gl.createProgram();
    gl.attachShader(program,shader(gl.VERTEX_SHADER,vertex));
    gl.attachShader(program,shader(gl.FRAGMENT_SHADER,fragment));
    gl.linkProgram(program); gl.useProgram(program);
    //  셰이더 attribute/uniform 위치를 미리 조회
    var loc={p:gl.getAttribLocation(program,'p'),n:gl.getAttribLocation(program,'n'),m:gl.getUniformLocation(program,'m'),pv:gl.getUniformLocation(program,'pv'),c:gl.getUniformLocation(program,'c')};

    //  05. 정점 위치·법선·인덱스를 GPU 버퍼로 업로드하는 공통 함수
    function mesh(pos,nor,idx){
        var a=gl.createBuffer(),b=gl.createBuffer(),e=gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER,a); gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(pos),gl.STATIC_DRAW);
        gl.bindBuffer(gl.ARRAY_BUFFER,b); gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(nor),gl.STATIC_DRAW);
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER,e); gl.bufferData(gl.ELEMENT_ARRAY_BUFFER,new Uint16Array(idx),gl.STATIC_DRAW);
        return {a:a,b:b,e:e,count:idx.length};
    }

    //  06. 퍼즐 조각 메시 생성: 탭(+1)과 홈(-1)이 서로 맞물리는 하나의 압출 입체 실루엣
    /* A single watertight extruded silhouette per piece. Positive edges are tabs,
       negative edges are matching sockets. Adjacent pieces share one exact profile. */
    //  퍼즐 조각의 폭/높이/두께/돌출 크기와 곡선 분할 수
    var PIECE_W=1.22, PIECE_H=.92, DEPTH=.16, TAB=.24, CURVE_STEPS=8;
    //  지정한 변(edge)과 탭 유형(type), 진행률(u)에 해당하는 2D 윤곽 좌표 계산
    function edgePoint(edge,type,u){
        var bump=0;
        if(type && u>.25 && u<.75) bump=type*TAB*Math.sin((u-.25)*Math.PI/.5);
        if(edge===0) return [-PIECE_W/2+PIECE_W*u, PIECE_H/2+bump];
        if(edge===1) return [PIECE_W/2+bump, PIECE_H/2-PIECE_H*u];
        if(edge===2) return [PIECE_W/2-PIECE_W*u, -PIECE_H/2-bump];
        return [-PIECE_W/2-bump, -PIECE_H/2+PIECE_H*u];
    }
    //  네 변의 좌표를 이어 한 조각의 폐곡선 윤곽 생성
    function contour(edges){
        var out=[];
        for(var e=0;e<4;e++){
            for(var s=0;s<CURVE_STEPS;s++) out.push(edgePoint(e,edges[e],s/CURVE_STEPS));
        }
        return out;
    }
    //  다각형 방향·외적·점의 삼각형 포함 여부를 계산하는 기하 보조 함수
    function area(poly){var a=0;for(var i=0;i<poly.length;i++){var q=poly[i],r=poly[(i+1)%poly.length];a+=q[0]*r[1]-r[0]*q[1];}return a*.5;}
    function cross(a,b,c){return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);}
    function inTri(p,a,b,c){var x=cross(a,b,p),y=cross(b,c,p),z=cross(c,a,p);return (x>=-1e-6&&y>=-1e-6&&z>=-1e-6)||(x<=1e-6&&y<=1e-6&&z<=1e-6);}
    //  Ear clipping 방식으로 앞/뒤 면을 삼각형 인덱스로 분할
    function triangulate(poly){
        var order=[]; for(var i=0;i<poly.length;i++) order.push(i);
        if(area(poly)<0) order.reverse();
        var tris=[],guard=0;
        while(order.length>2 && guard++<10000){
            var cut=false;
            for(i=0;i<order.length;i++){
                var ia=order[(i+order.length-1)%order.length],ib=order[i],ic=order[(i+1)%order.length];
                if(cross(poly[ia],poly[ib],poly[ic])<=1e-7) continue;
                var blocked=false;
                for(var j=0;j<order.length;j++){var ip=order[j];if(ip!==ia&&ip!==ib&&ip!==ic&&inTri(poly[ip],poly[ia],poly[ib],poly[ic])){blocked=true;break;}}
                if(!blocked){tris.push(ia,ib,ic);order.splice(i,1);cut=true;break;}
            }
            if(!cut) break;
        }
        return tris;
    }
    //  2D 윤곽을 앞면·뒷면·옆면이 있는 3D 퍼즐 조각 mesh로 변환
    function puzzleMesh(edges){
        var poly=contour(edges), count=poly.length, pos=[],nor=[],idx=[],front=triangulate(poly);
        for(var i=0;i<count;i++){pos.push(poly[i][0],poly[i][1],DEPTH);nor.push(0,0,1);}
        for(i=0;i<count;i++){pos.push(poly[i][0],poly[i][1],-DEPTH);nor.push(0,0,-1);}
        for(i=0;i<front.length;i+=3){idx.push(front[i],front[i+1],front[i+2]);idx.push(count+front[i+2],count+front[i+1],count+front[i]);}
        for(i=0;i<count;i++){
            var k=(i+1)%count,a=poly[i],b=poly[k],dx=b[0]-a[0],dy=b[1]-a[1],len=Math.sqrt(dx*dx+dy*dy)||1;
            var nx=dy/len,ny=-dx/len,o=pos.length/3;
            pos.push(a[0],a[1],DEPTH,b[0],b[1],DEPTH,b[0],b[1],-DEPTH,a[0],a[1],-DEPTH);
            for(var n=0;n<4;n++) nor.push(nx,ny,0);
            idx.push(o,o+1,o+2,o,o+2,o+3);
        }
        return mesh(pos,nor,idx);
    }

    //  07. 3D 행렬 계산: 이동·크기·X/Y/Z 회전·원근 투영·최종 모델 행렬
    /* Explicit column-major transforms keep the parent animation auditable. */
    function identity(){return new Float32Array([1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]);}
    function mul(a,b){var o=new Float32Array(16);for(var c=0;c<4;c++)for(var r=0;r<4;r++)o[c*4+r]=a[r]*b[c*4]+a[4+r]*b[c*4+1]+a[8+r]*b[c*4+2]+a[12+r]*b[c*4+3];return o;}
    function T(x,y,z){var m=identity();m[12]=x;m[13]=y;m[14]=z;return m;}
    function S(x,y,z){var m=identity();m[0]=x;m[5]=y;m[10]=z;return m;}
    function RX(v){var c=Math.cos(v),s=Math.sin(v);return new Float32Array([1,0,0,0,0,c,s,0,0,-s,c,0,0,0,0,1]);}
    function RY(v){var c=Math.cos(v),s=Math.sin(v);return new Float32Array([c,0,-s,0,0,1,0,0,s,0,c,0,0,0,0,1]);}
    function RZ(v){var c=Math.cos(v),s=Math.sin(v);return new Float32Array([c,s,0,0,-s,c,0,0,0,0,1,0,0,0,0,1]);}
    function P(f,a,n,z){var t=1/Math.tan(f/2),q=1/(n-z);return new Float32Array([t/a,0,0,0,0,t,0,0,0,0,(z+n)*q,-1,0,0,2*z*n*q,0]);}
    function M(pos,rot,scale){return mul(mul(mul(mul(T(pos[0],pos[1],pos[2]),RY(rot[1])),RX(rot[0])),RZ(rot[2])),S(scale[0],scale[1],scale[2]));}

    //  08. 여섯 조각의 상/우/하/좌 변 형태 정의: 0=직선, 1=돌출 탭, -1=들어간 홈
    var edgeSets=[
        [0, 1, 1,0], [0,-1,-1,-1], [0,0, 1, 1],
        [-1,-1,0,0], [1, 1,0, 1], [-1,0,0,-1]
    ];
    //  조립 완료 시 각 퍼즐 조각이 도착할 3×2 목표 좌표
    var targets=[[-PIECE_W,PIECE_H/2,0],[0,PIECE_H/2,0],[PIECE_W,PIECE_H/2,0],[-PIECE_W,-PIECE_H/2,0],[0,-PIECE_H/2,0],[PIECE_W,-PIECE_H/2,0]];
    //  조립 전 화면 밖/주변에 흩어진 각 조각의 시작 좌표
    var starts=[[-3.45,2.35,-1.4],[3.2,2.5,-1.8],[3.65,.25,-1.2],[-3.55,-2.15,-1.5],[.1,-2.75,-1.8],[3.4,-2.1,-1.25]];
    //  각 조각의 RGB 색상: 시안·블루·라벤더·민트·스카이블루·바이올렛
    var colors = [
        [0.26, 0.78, 0.95], // refined cyan
        [0.31, 0.58, 0.93], // clear blue
        [0.62, 0.54, 0.91], // lavender
        [0.29, 0.77, 0.68], // mint: kept luminous under top lighting
        [0.34, 0.67, 0.94], // sky blue
        [0.70, 0.57, 0.93]  // soft violet
    ];
    //  실제 퍼즐 객체 생성: mesh, 목표/시작 위치, 보간 진행률, 목표 상태, 초기 회전값 보관
    var pieces=targets.map(function(t,i){return{mesh:puzzleMesh(edgeSets[i]),t:t,s:starts[i],p:0,goal:0,spin:[-.6+i*.11,i%2?-.95:.92,i*.22]};});

    //  09. 장면 상태: 현재 단계, 시간, 전체 회전, 드래그 여부, 포인터 위치, 모션 감소 설정
    var stage=-1,started=Infinity,last=performance.now(),rx=-.17,ry=.23,tx=rx,ty=ry,drag=false,moved=false,px=0,py=0;
    var reduce=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    //  홀로그램 전환 상태: 연속 클릭 중에는 마지막 요청 단계를 보관해 순서가 꼬이지 않게 처리
    var currentHologramIndex=0,hologramTransitioning=false,pendingHologramIndex=null,hologramTimer=0;
    var hologramTransitionDuration=reduce?160:780;
    function hologramStageNumber(index){return String(index+1).padStart(2,'0');}
    //  단계 번호는 이전/다음 값을 겹쳐 올려 실제 카운트업처럼 전환하고, 하단 점도 함께 이동
    function updateHologramIndicator(index,animate){
        if(hologramNumber){
            var nextNumber=hologramStageNumber(index);
            var previousNumber=hologramNumber.textContent.trim()||hologramStageNumber(currentHologramIndex);
            hologramNumber.setAttribute('data-hologram-from',previousNumber);
            hologramNumber.setAttribute('data-hologram-to',nextNumber);
            hologramNumber.classList.toggle('is-counting',Boolean(animate&&!reduce&&previousNumber!==nextNumber));
            if(!animate||reduce) hologramNumber.textContent=nextNumber;
        }
        hologramDots.forEach(function(dot,i){dot.classList.toggle('is-active',i===index);});
    }
    //  에너지 펄스 → 약한 수평 슬라이스 → 스캔 위치를 따라 다음 화면이 공개되는 780ms 전환
    function switchHologram(nextIndex){
        if(!hologram||!hologramPanels.length) return;
        nextIndex=Math.max(0,Math.min(hologramPanels.length-1,nextIndex));
        if(hologramTransitioning){pendingHologramIndex=nextIndex;return;}
        if(nextIndex===currentHologramIndex){updateHologramIndicator(nextIndex);return;}

        var outgoing=hologramPanels[currentHologramIndex];
        var incoming=hologramPanels[nextIndex];
        hologramTransitioning=true;
        pendingHologramIndex=null;
        window.clearTimeout(hologramTimer);
        hologram.classList.add('is-switching');
        outgoing.setAttribute('data-hologram-state','outgoing');
        outgoing.setAttribute('aria-hidden','true');
        incoming.setAttribute('data-hologram-state','incoming');
        incoming.setAttribute('aria-hidden','false');
        updateHologramIndicator(nextIndex,true);

        hologramTimer=window.setTimeout(function(){
            outgoing.setAttribute('data-hologram-state','hidden');
            outgoing.classList.remove('is-active');
            incoming.setAttribute('data-hologram-state','active');
            incoming.classList.add('is-active');
            currentHologramIndex=nextIndex;
            hologramTransitioning=false;
            hologram.classList.remove('is-switching');
            if(hologramNumber){
                hologramNumber.textContent=hologramStageNumber(nextIndex);
                hologramNumber.classList.remove('is-counting');
            }
            hologram.dispatchEvent(new CustomEvent('hologram-transition-end',{detail:{index:nextIndex,number:hologramStageNumber(nextIndex)}}));

            if(pendingHologramIndex!==null&&pendingHologramIndex!==currentHologramIndex){
                var queuedIndex=pendingHologramIndex;
                pendingHologramIndex=null;
                switchHologram(queuedIndex);
            }else{
                pendingHologramIndex=null;
            }
        },hologramTransitionDuration);
    }
    //  클릭 단계 변경: 조립할 조각 수를 정하고 단계 문구/홀로그램/진행 바를 갱신
    function setStage(next){
        stage=next;pieces.forEach(function(p,i){p.goal=i<=stage?1:0;});
        var data=stages[Math.max(0,stage)];
        if(label){label.parentNode.classList.add('is-changing');window.setTimeout(function(){label.textContent=stage<0?'빈 공간을 눌러 시작':data.label;label.parentNode.classList.remove('is-changing');},140);}
        switchHologram(stage<0?0:stage);
        if(bar)bar.style.width=(stage<0?0:(stage+1)/6*100)+'%';
    }
    //  10. WebGL 렌더링 보조 함수: 버퍼 바인딩, 한 조각 그리기, canvas 해상도 동기화
    function bind(m){gl.bindBuffer(gl.ARRAY_BUFFER,m.a);gl.enableVertexAttribArray(loc.p);gl.vertexAttribPointer(loc.p,3,gl.FLOAT,false,0,0);gl.bindBuffer(gl.ARRAY_BUFFER,m.b);gl.enableVertexAttribArray(loc.n);gl.vertexAttribPointer(loc.n,3,gl.FLOAT,false,0,0);gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER,m.e);}
    function draw(m,model,color){bind(m);gl.uniformMatrix4fv(loc.m,false,model);gl.uniform3fv(loc.c,color);gl.drawElements(gl.TRIANGLES,m.count,gl.UNSIGNED_SHORT,0);}
    function resize(){var d=Math.min(devicePixelRatio||1,2),w=Math.max(1,canvas.clientWidth*d|0),h=Math.max(1,canvas.clientHeight*d|0);if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;gl.viewport(0,0,w,h);}}

    //  11. 사용자 상호작용 대상: HERO 빈 공간에서만 클릭/드래그를 받고 버튼·카드·헤더는 제외
    var interaction=document.querySelector('.hero--interactive')||canvas;
    //  클릭 가능한 빈 영역인지 판별
    function interactiveTarget(target){return !target.closest('a,button,input,select,textarea,.hero__copy,.hero__card-wrap,.user-menu,.site-header');}
    //  pointerdown: 드래그 시작 위치 저장 및 포인터 캡처
    interaction.addEventListener('pointerdown',function(e){if(!interactiveTarget(e.target))return;drag=true;moved=false;px=e.clientX;py=e.clientY;if(interaction.setPointerCapture)interaction.setPointerCapture(e.pointerId);});
    //  pointermove: 이동량으로 전체 퍼즐의 X/Y 회전 목표값 변경
    interaction.addEventListener('pointermove',function(e){if(!drag)return;var dx=e.clientX-px,dy=e.clientY-py;if(Math.abs(dx)+Math.abs(dy)>3)moved=true;ty+=dx*.008;tx=Math.max(-.72,Math.min(.5,tx+dy*.008));px=e.clientX;py=e.clientY;});
    //  pointerup/pointercancel: 드래그 상태 종료
    interaction.addEventListener('pointerup',function(){drag=false;});
    interaction.addEventListener('pointercancel',function(){drag=false;});
    //  단순 클릭이면 다음 조립 단계로 이동하고, 6단계 이후에는 초기 상태로 순환
    interaction.addEventListener('click',function(e){if(moved||!interactiveTarget(e.target))return;setStage(stage>=5?-1:stage+1);});

    //  12. 매 프레임 실행되는 WebGL 렌더 루프
    function render(now){
        //  canvas 크기 보정, 프레임 시간 계산, 각 조각 조립 진행률과 전체 회전값을 부드럽게 보간
        resize();
        var dt=Math.min(40,now-last)/1000; last=now;
        pieces.forEach(function(p){p.p+=(p.goal-p.p)*Math.min(1,dt*4.8);});
        rx+=(tx-rx)*.07; ry+=(ty-ry)*.07;
        //  깊이 테스트 활성화, 양면 표시, 투명 배경으로 프레임 초기화
        gl.enable(gl.DEPTH_TEST); gl.disable(gl.CULL_FACE);
        gl.clearColor(0,0,0,0); gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
        //  화면 비율에 맞는 원근 투영 행렬과 카메라 거리 설정
        var aspect=canvas.width/canvas.height;
        gl.uniformMatrix4fv(loc.pv,false,mul(P(Math.PI/4.2,aspect,.1,100),T(0,0,-7.5)));

        //  조립 상태와 무관하게 계속 유지되는 전체 퍼즐의 부유·호흡·미세 회전 모션
        /* Perpetual parent motion: never multiplied by assembly progress. It remains
           active in READY, assembling, complete, manual-drag and paused autoplay states. */
        var idle=reduce ? .25 : 1;
        var floatY=Math.sin(now*.00072)*.07*idle;
        var driftX=Math.cos(now*.00047)*.035*idle;
        var breathe=1+Math.sin(now*.00061)*.009*idle;
        var globalRot=[rx+Math.sin(now*.00039)*.025*idle,ry+Math.cos(now*.00033)*.035*idle,Math.sin(now*.00028)*.018*idle];
        var global=M([driftX,floatY,0],globalRot,[breathe,breathe,breathe]);

        //  각 조각을 시작 위치에서 목표 위치로 이동시키고, 미조립 상태에는 파동/회전 모션을 추가해 그리기
        pieces.forEach(function(p,i){
            var e=1-Math.pow(1-p.p,3),free=1-e,wave=Math.sin(now*.00082+i*1.31),sway=Math.cos(now*.00057+i*.87);
            var pos=[p.s[0]+(p.t[0]-p.s[0])*e+wave*.17*free,p.s[1]+(p.t[1]-p.s[1])*e+sway*.15*free,p.s[2]+(p.t[2]-p.s[2])*e+wave*.12*free];
            var rot=[p.spin[0]*free+wave*.10*free,p.spin[1]*free+now*.00022*free,p.spin[2]*free+sway*.08*free];
            draw(p.mesh,mul(global,M(pos,rot,[1,1,1])),colors[i]);
        });
        requestAnimationFrame(render);
    }
    //  13. 로더 종료 후 장면 시작 상태를 한 번만 설정
    function beginScene(){if(started!==Infinity)return;started=performance.now();setStage(-1);}
    //  초기 단계 설정 및 렌더 루프 즉시 시작; 로더 이벤트 또는 12초 fallback으로 beginScene 호출
    setStage(-1); requestAnimationFrame(render);
    if(window.__jobPuzzleLoaderFinished) beginScene();
    else {document.addEventListener('jobpuzzle:loaderclosed',beginScene,{once:true});window.setTimeout(beginScene,12000);}
})();

