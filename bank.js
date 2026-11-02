/* Testriq mock-test bank · Designed and developed by Md Sajid Shahriar Bhuiyan (SYMETREE)
   Original, procedurally generated practice sets. Not official exam content. */
(()=>{
const RNG=s=>()=>(s=(s*1664525+1013904223)>>>0)/4294967296,
sh=(a,r)=>{a=[...a];for(let i=a.length;i>1;){let j=Math.floor(r()*i--);[a[i],a[j]]=[a[j],a[i]]}return a},
ri=(r,a,b)=>a+Math.floor(r()*(b-a+1)),pk=(a,r)=>a[Math.floor(r()*a.length)],
LV=["Foundation","Intermediate","Advanced"];
function mc(r,prompt,right,wr,exp,tag){let w=[...new Set(wr)].filter(x=>x!==right);for(let k=1;w.length<3;k++)if(typeof right==="number"&&!w.includes(right+k))w.push(right+k);
 let o=sh([right,...w.slice(0,3)],r).map(String);return{prompt,options:o,answer:o.indexOf(String(right)),explanation:exp,tag}}
const MATH=[
r=>{let x=ri(r,2,12),a=ri(r,2,9),b=ri(r,1,20),c=a*x+b;return mc(r,`If ${a}x + ${b} = ${c}, what is the value of x?`,x,[x+1,x-2,c-b],`Subtract ${b} from both sides to get ${a}x = ${c-b}, then divide by ${a}: x = ${x}.`,"Linear equations")},
r=>{let P=ri(r,4,20)*10,d=pk([10,15,20,25,30],r),A=P*(100-d)/100;return mc(r,`A jacket costs $${P}. It is discounted by ${d}%. What is the sale price in dollars?`,A,[P*d/100,P*(100+d)/100,P-d],`Sale price = ${P} × ${(100-d)/100} = ${A}.`,"Percentages")},
r=>{let m=pk([-3,-2,-1,2,3,4,5],r),x1=ri(r,-3,3),dx=ri(r,1,4),y1=ri(r,-4,4);return mc(r,`A line passes through (${x1}, ${y1}) and (${x1+dx}, ${y1+m*dx}). What is its slope?`,m,[-m,m+1,m-1],`Slope = change in y ÷ change in x = ${m*dx} ÷ ${dx} = ${m}.`,"Lines & slope")},
r=>{let m=ri(r,10,30),n=[1,2,3,4].map(()=>ri(r,m-6,m+6)),L=5*m-n.reduce((a,b)=>a+b,0);return mc(r,`The mean of five numbers is ${m}. Four of them are ${n.join(", ")}. What is the fifth number?`,L,[L+2,L-3,m],`The sum must be 5 × ${m} = ${5*m}. Subtract the four known numbers to get ${L}.`,"Statistics")},
r=>{let p=ri(r,1,6),q=ri(r,p+1,9);return mc(r,`What is the sum of the solutions of x² − ${p+q}x + ${p*q} = 0?`,p+q,[p*q,p+q+1,-(p+q)],`The equation factors as (x − ${p})(x − ${q}) = 0, so the solutions are ${p} and ${q}; their sum is ${p+q}.`,"Quadratics")},
r=>{let a=ri(r,2,5),b=ri(r,1,9),k=ri(r,2,6);return mc(r,`If f(x) = ${a}x² + ${b}, what is f(${k})?`,a*k*k+b,[a*a*k*k+b,a*k+b,a*k*k-b],`f(${k}) = ${a}(${k*k}) + ${b} = ${a*k*k+b}.`,"Functions")},
r=>{let a=ri(r,1,4),b=ri(r,a+1,6),k=ri(r,2,8);return mc(r,`Two numbers are in the ratio ${a}:${b} and total ${(a+b)*k}. What is the larger number?`,b*k,[a*k,b*k+k,(a+b)*k-a*k-k],`One part is ${(a+b)*k} ÷ ${a+b} = ${k}. The larger number is ${b} × ${k} = ${b*k}.`,"Ratios")},
r=>{let q=ri(r,3,9),t=ri(r,4,15);return mc(r,`A tank holds ${q*t} liters and fills at ${q} liters per minute. How many minutes to fill it?`,t,[t+2,t-1,q*t-q],`Time = volume ÷ rate = ${q*t} ÷ ${q} = ${t} minutes.`,"Rates")},
r=>{let x=ri(r,3,12),y=ri(r,1,x-1);return mc(r,`If x + y = ${x+y} and x − y = ${x-y}, what is x?`,x,[y,x+1,x-1],`Add the equations: 2x = ${2*x}, so x = ${x}.`,"Systems")}];
const G=`Neither of the candidates ___ prepared for the interview.|was|were|are|have been|“Neither” takes a singular verb.
She has lived in Dhaka ___ 2019.|since|for|from|during|Use “since” with a starting point in time.
If I ___ more time, I would travel abroad.|had|have|will have|would have|Second conditional: if + past simple.
He suggested ___ the meeting until Friday.|postponing|to postpone|postpone|to postponing|“Suggest” is followed by a gerund.
The number of applicants ___ increased sharply.|has|have|are|were|“The number of” is singular.
She is good ___ solving complex problems.|at|in|on|for|The fixed phrase is “good at”.
By the time we arrived, the film ___.|had started|has started|starts|was start|Past perfect marks the earlier past action.
I look forward to ___ from you.|hearing|hear|heard|have heard|“Look forward to” is followed by -ing.
Hardly ___ the room when the alarm rang.|had she entered|she had entered|she entered|did she entered|Negative adverbs at the start trigger inversion.
The research, ___ was funded by the government, was published online.|which|that|who|what|Non-defining clauses use “which”, not “that”.
Each of the students ___ a separate laptop.|has|have|having|are having|“Each” takes a singular verb.
Many of the villagers ___ to leave their homes.|were forced|was forced|forces|has forced|Plural subject with a passive verb.
She asked me where ___.|I had been|had I been|was I been|I have been|Indirect questions keep statement word order.
We would have won if we ___ harder.|had trained|trained|would train|have trained|Third conditional: if + past perfect.
The more you practise, ___ you become.|the more confident|more confident|most confident|the most confident|Comparative correlative structure.
Despite ___ tired, he finished the report.|being|he was|to be|of being|“Despite” is followed by a noun or -ing form.
There ___ several reasons for the delay.|were|was|is|has|“There” agrees with the plural noun that follows.
I’d rather you ___ me the truth.|told|tell|had tell|will tell|“Would rather” + subject takes the past simple.
The report, along with its appendices, ___ available online.|is|are|were|have been|The subject is “report”; “along with” does not make it plural.
She apologised for ___ late.|being|be|to be|been|A preposition is followed by -ing.
Not only ___ the exam, but she also won a scholarship.|did she pass|she passed|she did pass|passed she|“Not only” at the start triggers inversion.
He denied ___ the document.|taking|to take|take|to have take|“Deny” is followed by a gerund.
Unless you ___ now, you will miss the train.|leave|will leave|don’t leave|would leave|“Unless” already means “if not”.
The committee ___ its decision yesterday.|announced|has announced|announces|have announce|A finished time word needs the past simple.`.split("\n").map(l=>l.split("|"));
const V=`mitigate|reduce the severity of|increase|ignore|celebrate
ubiquitous|found everywhere|extremely rare|very expensive|easily broken
scrutinise|examine closely|avoid|praise|copy
ambiguous|open to more than one meaning|completely clear|very old|highly dangerous
resilient|able to recover quickly|easily upset|very slow|deeply hidden
diminish|become smaller|become louder|become older|become clearer
substantiate|support with evidence|deny|postpone|exaggerate
reluctant|unwilling|eager|careless|generous
inevitable|certain to happen|easy to avoid|badly designed|recently invented
coherent|logical and consistent|confusing|colourful|temporary
advocate|publicly support|strongly oppose|accidentally lose|quietly observe
prevalent|widespread|hidden|outdated|expensive
meticulous|very careful about detail|lazy|noisy|friendly
feasible|possible to do|impossible to explain|very costly|unnecessarily complex
detrimental|harmful|helpful|neutral|temporary
fluctuate|rise and fall irregularly|stay the same|rise steadily|disappear`.split("\n").map(l=>l.split("|"));
const gq=(r,x)=>mc(r,x[0],x[1],x.slice(2,5),x[5],"Grammar"),vq=(r,x)=>mc(r,`In an academic text, the word “${x[0]}” most nearly means:`,x[1],x.slice(2,5),`“${x[0]}” means ${x[1]}.`,"Vocabulary");
const out=[];let seed=100;
const add=(exam,skill,kind,n,label,desc,dur,build)=>{for(let i=0;i<n;i++){let r=RNG(seed+=7919);out.push({id:`${exam}-${kind}-${i+1}`.toLowerCase(),exam,skill,level:LV[i%3],duration:dur,title:`${label} · Set ${String(i+1).padStart(2,"0")}`,desc,questions:build(r)})}};
const pool=(a,f)=>r=>sh(a,r).slice(0,8).map(x=>f(r,x));
add("SAT","Math","math",14,"SAT Math","Algebra, data analysis and problem solving with fresh numbers in every set.",16,r=>sh(MATH.concat(MATH[0],MATH[1],MATH[2]),r).slice(0,8).map(f=>f(r)));
add("IELTS","Grammar","gram",5,"IELTS Grammar","Sentence accuracy for Writing and Speaking: tenses, agreement, inversion.",8,pool(G,gq));
add("IELTS","Grammar","vocab",4,"IELTS Vocabulary","Academic vocabulary that lifts Reading and Writing scores.",8,pool(V,vq));
add("SAT","Grammar","conv",4,"SAT R&W Conventions","Standard English conventions in the digital SAT style.",8,pool(G,gq));
add("SAT","Reading","wic",4,"SAT R&W Words in Context","Choose the meaning that fits an academic sentence.",8,pool(V,vq));
add("PTE","Reading","fib",4,"PTE Fill in the Blanks","Grammar-driven gap fill for PTE Reading & Writing.",8,pool(G,gq));
add("PTE","Reading","vocab",3,"PTE Vocabulary","Word meaning practice for PTE Reading.",8,pool(V,vq));
const T=["travel","technology","education","music","food and cooking","health and exercise","public transport","friendship","reading","the environment","work and careers","festivals","social media","sports","shopping","weather and seasons"];
const RA=["Urban planners increasingly argue that walkable neighbourhoods improve both public health and community life.","Although renewable energy has become cheaper, many countries still rely heavily on imported fossil fuels.","Sleep researchers have found that irregular schedules can reduce concentration and weaken long-term memory.","The university library offers extended opening hours during the examination period for all enrolled students.","Museums now use digital tools to help visitors understand objects that were once only displayed behind glass."];
const cap=t=>t[0].toUpperCase()+t.slice(1);
T.forEach((t,i)=>out.push({id:`ielts-speaking-mock-${i+1}`,exam:"IELTS",skill:"Speaking",level:LV[i%3],duration:14,title:`Speaking Mock ${String(i+1).padStart(2,"0")} · ${cap(t)}`,desc:"Full 3-part interview with AI Speaking Coach feedback.",mode:"speaking",prompts:[
{part:"Part 1 · Interview",time:"20–30 seconds",text:`Let’s talk about ${t}. How important is it in your daily life?`},
{part:"Part 1 · Interview",time:"20–30 seconds",text:`How has your relationship with ${t} changed since you were a child?`},
{part:"Part 2 · Cue card",time:"Prepare 1 min · speak 1–2 min",text:`Describe a memorable experience connected with ${t}. You should say: when it happened, who was involved, what happened, and why it was memorable.`},
{part:"Part 3 · Discussion",time:"30–45 seconds",text:`How does ${t} differ between older and younger generations?`},
{part:"Part 3 · Discussion",time:"30–45 seconds",text:`What role should governments play in relation to ${t}, and why?`}]}));
T.slice(0,10).forEach((t,i)=>out.push({id:`pte-speaking-mock-${i+1}`,exam:"PTE",skill:"Speaking",level:LV[i%3],duration:10,title:`PTE Speaking Mock ${String(i+1).padStart(2,"0")} · ${cap(t)}`,desc:"Read aloud with accuracy check, plus situation and opinion responses.",mode:"speaking",prompts:[
{part:"Read Aloud",time:"Read within 40 seconds",text:RA[i%5]},
{part:"Read Aloud",time:"Read within 40 seconds",text:RA[(i+2)%5]},
{part:"Respond to a situation",time:"40 seconds",text:`A friend is struggling with ${t} and asks for your advice. Explain what you would suggest and why.`},
{part:"Opinion",time:"40 seconds",text:`Do you agree that ${t} will become more important in the next ten years? Give reasons and examples.`}]}));

// Additional original IELTS mini-mocks: varied reading and listening practice.
const IELTS_READINGS=[
 {topic:"Community Libraries",passage:"COMMUNITY LIBRARIES\n\nModern community libraries offer more than books. Many provide quiet study areas, digital access, language classes and workshops. These services can be particularly valuable for people who lack reliable internet access at home. Yet libraries face pressure from rising costs and changing expectations. Some have responded by partnering with schools and local organisations, while others extend opening hours during examination periods. Their success depends not only on the size of their collections but also on whether residents can reach and use the services.",qs:[
 ["What is the passage mainly about?",["Libraries provide varied community services and face changing needs.","Libraries are replacing every school.","Internet access is no longer useful.","Most residents prefer buying books."],0,"The passage outlines several library services and the pressures they face."],
 ["Why can library digital access be valuable?",["It replaces all classroom teaching.","Some people lack reliable internet at home.","It makes books unnecessary.","It reduces the need for staff."],1,"The passage explicitly mentions people without reliable home internet."],
 ["Libraries' success depends only on collection size.",["True","False","Not Given"],1,"The passage says success depends not only on collections but also on access and use."]
 ]},
 {topic:"Urban Cycling",passage:"URBAN CYCLING\n\nCities have invested in cycling lanes to reduce congestion and encourage physical activity. A connected network may make short journeys more practical, but isolated lanes can leave riders uncertain at intersections. Safety also depends on traffic speed, visibility and driver behaviour. Surveys suggest that people who do not currently cycle are often more concerned about safety than journey time. For this reason, transport planners increasingly assess complete routes rather than counting kilometres of new lanes alone.",qs:[
 ["What is the central point?",["Cycling routes should be planned as connected, safe networks.","Cycling is always faster than driving.","Only experienced riders use city lanes.","Journey time is the sole concern."],0,"The text argues for connected routes and broader safety considerations."],
 ["What concern is common among people who do not cycle?",["The price of bicycles","Safety","Weather forecasts","Finding parking"],1,"The passage says they are often more concerned about safety."],
 ["Isolated cycling lanes may cause uncertainty at intersections.",["True","False","Not Given"],0,"This is directly stated in the passage."]
 ]},
 {topic:"Food Waste",passage:"REDUCING FOOD WASTE\n\nHouseholds waste food for several reasons, including confusing date labels, poor meal planning and buying more than they need. Community projects have responded with cooking classes and food-sharing schemes. Retailers can help by improving storage advice and offering smaller package sizes. However, no single intervention is likely to solve the problem everywhere because shopping habits, income and local infrastructure differ. Researchers therefore recommend measuring outcomes before expanding a programme.",qs:[
 ["Which factor contributes to household food waste?",["Confusing date labels","Too many public parks","Lack of public transport only","Excessive rainfall"],0,"Confusing date labels are listed among the causes."],
 ["Why may one solution not work everywhere?",["Local circumstances differ.","Food waste is impossible to measure.","Retailers cannot change packaging.","All households shop identically."],0,"Shopping habits, income and infrastructure vary."],
 ["Researchers recommend evaluating a programme before expanding it.",["True","False","Not Given"],0,"The final sentence recommends measuring outcomes first."]
 ]},
 {topic:"Museum Technology",passage:"MUSEUMS AND DIGITAL TOOLS\n\nMuseums increasingly use audio guides, interactive maps and digital reconstructions to help visitors interpret collections. These tools can provide context that a label cannot easily convey. They are not equally suitable for every visitor, however, and poorly designed screens may distract from original objects. Curators often combine digital interpretation with guided tours and printed information. The aim is not to make technology the main attraction but to make the collection more understandable and accessible.",qs:[
 ["What is the purpose of digital tools in museums?",["To help visitors interpret collections","To replace every curator","To prevent visitors seeing objects","To reduce historical accuracy"],0,"The tools provide context and improve understanding."],
 ["What risk is mentioned?",["Screens may distract from original objects.","Visitors will stop reading entirely.","Collections become larger.","Guides cannot speak."],0,"Poorly designed screens may distract visitors."],
 ["The passage suggests museums should use only digital interpretation.",["True","False","Not Given"],1,"It describes combining digital tools with tours and printed information."]
 ]},
 {topic:"Sleep and Learning",passage:"SLEEP AND LEARNING\n\nSleep supports memory consolidation, the process through which newly learned information becomes more stable. Students sometimes sacrifice sleep to gain extra study hours, yet fatigue can make concentration and recall more difficult. The ideal amount of sleep varies by age and individual, so a single schedule will not suit everyone. Researchers generally recommend consistent routines and realistic study plans rather than relying on last-minute revision alone.",qs:[
 ["What does sleep help support?",["Memory consolidation","Faster internet","Longer school holidays","Higher room temperature"],0,"The first sentence directly links sleep to memory consolidation."],
 ["Why can sacrificing sleep be counterproductive?",["Fatigue can reduce concentration and recall.","It guarantees better grades.","It removes the need to revise.","It makes routines unnecessary."],0,"Fatigue may impair concentration and recall."],
 ["A single sleep schedule suits everyone.",["True","False","Not Given"],1,"The passage says needs vary and one schedule will not suit everyone."]
 ]}
];
IELTS_READINGS.forEach((item,i)=>out.push({id:`ielts-reading-mock-${i+1}`,exam:"IELTS",skill:"Reading",level:LV[i%3],duration:12,title:`IELTS Reading Mock ${String(i+1).padStart(2,"0")} · ${item.topic}`,desc:"Original Academic-style passage with detail, inference and True/False/Not Given practice.",passage:item.passage,questions:item.qs.map((q,j)=>({prompt:q[0],options:q[1],answer:q[2],explanation:q[3],tag:j===2?"True / False / Not Given":"Reading comprehension"})).concat([{prompt:"Choose the best summary of the passage.",options:[`It discusses key ideas and practical considerations about ${item.topic.toLowerCase()}.`,"It argues that all public services should close.","It gives a fictional story without a main point.","It says individual choices never matter."],answer:0,explanation:"The passage explains the topic and its practical considerations.",tag:"Summary"}])}));
const IELTS_LISTENING=[
 {title:"University Enquiry",script:"Hello, Student Services. Hi, I'm calling about the academic writing workshop. It takes place on Wednesday at 2:15 in Room 18. There is no charge, but students need to register online by Monday. Please bring a notebook; printed handouts will be provided.",qs:[
 ["When is the workshop?",["Tuesday","Wednesday","Thursday","Friday"],1,"The workshop takes place on Wednesday."],
 ["What time does it begin?",["2:00","2:15","2:30","3:15"],1,"The speaker says 2:15."],
 ["How should students register?",["By telephone","In person","Online","By post"],2,"Students need to register online by Monday."],
 ["What should students bring?",["A laptop","A notebook","Printed handouts","A camera"],1,"The speaker asks them to bring a notebook."]
 ]},
 {title:"Train Information",script:"Attention passengers. The 09:40 service to Bristol is delayed by approximately fifteen minutes because of a signalling problem. Passengers for Bath should remain on the platform, as their connection will wait. The café near platform six is open, but the waiting room is temporarily closed for cleaning.",qs:[
 ["What is the cause of the delay?",["Heavy rain","A signalling problem","A staff meeting","Track repairs"],1,"The announcement gives a signalling problem as the cause."],
 ["How long is the delay expected to be?",["Five minutes","Ten minutes","Fifteen minutes","Thirty minutes"],2,"The train is delayed by approximately fifteen minutes."],
 ["Who should remain on the platform?",["Passengers for Bath","Passengers for London","Café staff","All drivers"],0,"Passengers for Bath are told to remain."],
 ["What is temporarily closed?",["The café","Platform six","The ticket office","The waiting room"],3,"The waiting room is closed for cleaning."]
 ]},
 {title:"Community Garden",script:"Welcome to the community garden orientation. Volunteers meet every Saturday at nine in the morning. Tools are provided, but please bring gloves and a water bottle. In summer we focus on watering before the temperature rises. New volunteers should sign the attendance sheet beside the greenhouse.",qs:[
 ["When do volunteers meet?",["Every Friday","Every Saturday","Every Sunday","Every morning"],1,"The meeting is every Saturday."],
 ["Which items should volunteers bring?",["Tools and seeds","Gloves and water","A lunch and notebook","A shovel only"],1,"Tools are provided; gloves and water are requested."],
 ["What is the summer priority?",["Planting trees","Painting the greenhouse","Watering early","Harvesting at night"],2,"Watering is done before the temperature rises."],
 ["Where is the attendance sheet?",["By the greenhouse","At the gate","In the office","Near the road"],0,"It is beside the greenhouse."]
 ]},
 {title:"Hotel Reservation",script:"Good afternoon, Lakeside Hotel. I'd like to reserve a double room for two nights, arriving on the 14th of May. Certainly. Breakfast is included, and check-in begins at three p.m. Could you note that we'll arrive late? Of course. Please call reception if you expect to arrive after ten p.m. Parking is available behind the building for guests.",qs:[
 ["What type of room is requested?",["Single","Double","Family suite","Twin cabin"],1,"The caller requests a double room."],
 ["How long will the guest stay?",["One night","Two nights","Three nights","A week"],1,"The caller says two nights."],
 ["What is included?",["Dinner","Airport transfer","Breakfast","Laundry"],2,"Breakfast is included."],
 ["Where is guest parking?",["In front of reception","Behind the building","Across the road","Underground"],1,"Parking is behind the building."]
 ]}
];
IELTS_LISTENING.forEach((item,i)=>out.push({id:`ielts-listening-mock-${i+1}`,exam:"IELTS",skill:"Listening",level:LV[i%3],duration:8,title:`IELTS Listening Mock ${String(i+1).padStart(2,"0")} · ${item.title}`,desc:"Listen to a spoken-style script and answer detail, number and location questions.",audioText:item.script,questions:item.qs.map(q=>({prompt:q[0],options:q[1],answer:q[2],explanation:q[3],tag:"Listening detail"}))}));

tests.push(...out);
courses.forEach(c=>c.count=tests.filter(t=>t.exam===c.exam).length+" practice sets");renderAll();
})();
