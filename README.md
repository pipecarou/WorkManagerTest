# WorkManagerTest

Ejemplo de como crear un long running worker con acceso a la ubicación del dispositivo.

Uso:
- Presionar el único botón que tiene la app.
- Debería aparecer una notificación persistente, con la ubicación actual del device.
- La ubicación se actualizará tantas veces como lo indique la const COUNTER_BREAK dentro de LongRunningWorker.
- Sean libres de modificar la lógica de corte o agregar un cancel action a la notification.
