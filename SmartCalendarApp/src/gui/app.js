
document.addEventListener("DOMContentLoaded", function () {
  const calendarElement = document.getElementById("calendar");

  const calendar = new FullCalendar.Calendar(calendarElement, {
    themeSystem: "monarch",
    initialView: "dayGridMonth",

    headerToolbar: {
      left: "prev,next today",
      center: "title",
      right: "dayGridMonth,timeGridWeek,timeGridDay"
    },

    eventClick: function (info) {
      alert("Event: " + info.event.title);
    }
  });

  calendar.render();
});
