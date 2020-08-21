package main

import (
	"license-server/service"

	"github.com/labstack/echo/v4"
)

func main() {
	service.SaveData()
	go service.ExecCronjob()
	e := echo.New()

	e.GET("/info", service.Gethwinfo)

<<<<<<< HEAD
<<<<<<< HEAD
	e.Logger.Fatal(e.Start(":9090"))
=======
	e.Logger.Fatal(e.Start(":8080"))
>>>>>>> 7b2d915... update
=======
	e.Logger.Fatal(e.Start(":9090"))
>>>>>>> 3b5a950... Update
}
