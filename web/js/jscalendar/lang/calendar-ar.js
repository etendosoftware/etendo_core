// ** I18N

// Calendar EN language
// Author: Mihai Bazon, <mihai_bazon@yahoo.com>
// Encoding: any
// Distributed under the same terms as the calendar itself.

// For translators: please use UTF-8 if possible.  We strongly believe that
// Unicode is the answer to a real internationalized world.  Also please
// include your contact information in the header, as can be seen above.

// full day names
Calendar._DN = new Array
("الأحد",
 "الإثنين",
 "الثلاثاء",
 "الأربعاء",
 "الخميس",
 "الجمعة",
 "السبت",
 "الأحد");

// Please note that the following array of short day names (and the same goes
// for short month names, _SMN) isn't absolutely necessary.  We give it here
// for exemplification on how one can customize the short day names, but if
// they are simply the first N letters of the full name you can simply say:
//
//   Calendar._SDN_len = N; // short day name length
//   Calendar._SMN_len = N; // short month name length
//
// If N = 3 then this is not needed either since we assume a value of 3 if not
// present, to be compatible with translation files that were written before
// this feature.

// short day names
Calendar._SDN = new Array
("الأحد",
 "الإثنين",
 "الثلاثاء",
 "الأربعاء",
 "الخميس",
 "الجمعة",
 "السبت",
 "الأحد");

// First day of the week. "0" means display Sunday first, "1" means display
// Monday first, etc.
Calendar._FD = 6;

// full month names
Calendar._MN = new Array
("يناير",
 "فبراير",
 "مارس",
 "أبريل",
 "مايو",
 "يونيو",
 "يوليو",
 "أغسطس",
 "سبتمبر",
 "أكتوبر",
 "نوفمبر",
 "ديسمبر");

// short month names
Calendar._SMN = new Array
("يناير",
 "فبراير",
 "مارس",
 "أبريل",
 "مايو",
 "يونيو",
 "يوليو",
 "أغسطس",
 "سبتمبر",
 "أكتوبر",
 "نوفمبر",
 "ديسمبر");
// tooltips

Calendar._TT = {};
Calendar._TT["INFO"] = "عن التقويم";

Calendar._TT["ABOUT"] =
"منتقي الوقت /التاريخ\n" +
"(c) dynarch.com 2002-2005 / Author: Mihai Bazon\n" + // don't translate this this ;-)
"للحصول على أحدث إصدار قم بزيارة: http://www.dynarch.com/projects/calendar/\n" +
"رخصة الاستخدام GNU LGPL.  انظر http://gnu.org/licenses/lgpl.html لمزيد من المعلومات." +
"\n\n" +
"اختر التاريخ:\n" +
"- استخدم مفتاحي \xab ، \xbb لإختيار السنة\n" +
"- استخدم مفتاحي " + String.fromCharCode(0x2039) + ", " + String.fromCharCode(0x203a) + " لإختيار الشهر\n" +
"- قم بضغط زر الفأرة على أي من المفاتيح العليا للإختيار السريع. ";
Calendar._TT["ABOUT_TIME"] = "\n\n" +
"اختيار التاريخ:\n" +
"- إضغط على أي جزء من الوقت لزيادتة\n" +
"- أو قم بالنقر مع مفتاح شيفت لإنقاصه \n" +
"- أو انقر واسحب للإختيار السريع.";

Calendar._TT["PREV_YEAR"] = "العام السابق (اضغط لعرض التقويم)";
Calendar._TT["PREV_MONTH"] ="الشهر السابق (اضغط لعرض التقويم)";
Calendar._TT["GO_TODAY"] = "اليوم";
Calendar._TT["NEXT_MONTH"] = "الشهر التالي (اضغط لعرض التقويم)";
Calendar._TT["NEXT_YEAR"] = " العام التالي (اضغط لعرض التقويم)";
Calendar._TT["SEL_DATE"] = "اختر التاريخ";
Calendar._TT["DRAG_TO_MOVE"] = "اسحب للنقل";
Calendar._TT["PART_TODAY"] = " اليوم";

// the following is to inform that "%s" is to be the first day of week
// %s will be replaced with the day name.
Calendar._TT["DAY_FIRST"] = "اعرض يوم %s أولا";

// This may be locale-dependent.  It specifies the week-end days, as an array
// of comma-separated numbers.  The numbers are from 0 to 6: 0 means Sunday, 1
// means Monday, etc.
Calendar._TT["WEEKEND"] = "0,6";

Calendar._TT["CLOSE"] = "إغلاق";
Calendar._TT["TODAY"] = "اليوم";
Calendar._TT["TIME_PART"] = "اضغط مفتاح (Shift) مع النقر او السحب لتغيير القيمة";

// date formats
Calendar._TT["DEF_DATE_FORMAT"] = "%Y-%m-%d";
Calendar._TT["TT_DATE_FORMAT"] = "%a, %b %e";

Calendar._TT["WK"] = "الاسبوع";
Calendar._TT["TIME"] = "الوقت:";