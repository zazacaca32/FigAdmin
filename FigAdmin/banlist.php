<?
header('Content-Type: text/html; charset=utf-8'); 
?>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<title>Banned Players of server</title>
</head>
<body>
<?php

// change these things

   $server = "localhost";
   $dbuser = "root";
   $dbpass = "root";
   $dbname = "minetest";
   $db = new mysqli($server, $dbuser, $dbpass, $dbname);
if($db->connect_errno > 0){
    die('Unable to connect to database [' . $db->connect_error . ']');
}

$result = $db->query("SELECT * FROM banlist ORDER BY time DESC");

echo "<table width=70% border=1 cellpadding=5 cellspacing=0>";

echo "<tr style=\"font-weight:bold\">
<td>Name</td>
<td>UUID</td>
<td>Reason</td>
<td>Admin/Mod</td>
<td>Time of ban</td>
<td>Time of unban</td>
</tr>";

while($row = $result->fetch_assoc()){

if($col == "#eeeeee"){
$col = "#ffffff";
}else{
$col = "#eeeeee";
}
echo "<tr bgcolor=$col>";

echo "<td>".$row['name']."</td>";
echo "<td>".$row['uuid']."</td>";
echo "<td>".$row['reason']."</td>";
echo "<td>".$row['admin']."</td>";
echo "<td>".$row['time']."</td>";
if($row['temptime'] == "0"){
echo "<td>∞</td>";
}else{
echo "<td>".$row['temptime']."</td>";
}

echo "</tr>";
}

echo"</table>"

?>
Ban database provided by 
<a href="http://dev.bukkit.org/server-mods/figadmin/">FigAdmin</a>.
</body></html>
