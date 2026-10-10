import { BOOKS } from "../src/curriculum-data.js";

const stages = ["learning", "spelling", "boss", "frog"];
const split = (value) => value.split("|").map((item) => item.trim()).filter(Boolean);

const expected = [
  {
    title: "Different friends",
    terms: split("team|player|him|lovely|clever|young|hard-working|science|robot|jump|rope|piano|chess|front|easy|star|wonderful|Australia"),
    phrases: split("be good at|work together|football team|jump rope|speak Chinese|play the piano|play Chinese chess|play the pipa|would like to|help a lot|wait for|in front of|Science and Art Festival|football match|come to life|what a pity")
  },
  {
    title: "My feelings",
    terms: split("sad|matter|project|centre|agree|finish|Monday|worried|both|excited|wrong|angry|afraid|parent|ask|move|yours"),
    phrases: split("look sad|be angry with|what's the matter|talk to your parents|school project|ask your teacher for help|children's centre|listen to your friends|clean up the park|agree with each other|share ideas|finish our project|look for|by Monday|make someone happy|use kind words|maths test|feel better|count to ten|try new things")
  },
  {
    title: "Work and play",
    terms: split("Wednesday|subject|Monday|Tuesday|Friday|because|Thursday|start|usually|weekend|sometimes|Saturday|Sunday|lesson|evening|answer|ping-pong|night|visit|grandparent"),
    phrases: split("have PE|go running|favourite subject|weekend activities|have pipa lessons|look after|tidy my room|do many chores|make a schedule|play ping-pong|visit grandparents|do homework on time|take breaks|stay healthy|eat well|get enough sleep|to-do list|balance work and play")
  },
  {
    title: "Healthy habits",
    terms: split("stay|should|flu|else|exercise|enough|if|habit|show|phone|then|rest|take care of|check|less|brush|tooth|twice|hour|free"),
    phrases: split("stay healthy|have a cold|wash hands|exercise every day|drink enough water|eat less candy|eat breakfast|rest your eyes|eat healthy food|brush your teeth|get enough sleep|take care of")
  },
  {
    title: "Food we eat",
    terms: split("hungry|beef|ice cream|little|dumpling|tea|hamburger|Mrs|coconut|interesting|seed|pull|pull up|stem|root|lotus|lake|world|round|different|way"),
    phrases: split("would like|beef noodles|a little|chicken soup|green tea|come from|plant seeds|grow from seeds|cook meals")
  },
  {
    title: "Nature and us",
    terms: split("river|nature|waterfall|forest|famous|mountain|wind|trip|dangerous|heavy|hotel|close|umbrella|raincoat|worry|fine|fog|flood|fire|know"),
    phrases: split("nature park|go hiking|think of|draw pictures|close the door|heavy rain|heavy snow|bad weather|be careful of|check the weather|wear good shoes|stay in the hotel|bring enough water|go on a trip")
  }
];

const book = BOOKS.find((item) => item.id === "pep_g5_first");
if (!book) throw new Error("Missing PEP Grade 5 first volume");
if (book.units.length !== expected.length) {
  throw new Error(`Expected ${expected.length} units, found ${book.units.length}`);
}

let checkedEntries = 0;
book.units.forEach((unit, unitIndex) => {
  const requirement = expected[unitIndex];
  if (unit.title !== requirement.title) {
    throw new Error(`Unit ${unit.number} title mismatch: "${unit.title}"`);
  }

  const normalized = unit.words.map((word) => word.word.trim().toLowerCase());
  if (new Set(normalized).size !== normalized.length) {
    throw new Error(`Unit ${unit.number} contains duplicate words or phrases`);
  }

  const requiredEntries = [...new Set([...requirement.terms, ...requirement.phrases].map((item) => item.toLowerCase()))];
  const missing = requiredEntries.filter((item) => !normalized.includes(item));
  if (missing.length) {
    throw new Error(`Unit ${unit.number} is missing: ${missing.join(", ")}`);
  }

  unit.words.forEach((word) => {
    const target = word.word.toLowerCase().replace(/[^a-z\s]/g, "").replace(/\s+/g, " ").trim();
    if (!target) throw new Error(`Empty spelling target for ${word.id}`);
    const missingStages = stages.filter((stage) => !word.practiceStages.includes(stage));
    if (missingStages.length) {
      throw new Error(`${word.id} missing practice stages: ${missingStages.join(", ")}`);
    }
  });
  checkedEntries += unit.words.length;
});

console.log(`Curriculum audit passed: ${book.units.length} units, ${checkedEntries} words/phrases, 4 practice stages.`);
