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

	e.Logger.Fatal(e.Start(":9090"))
}
