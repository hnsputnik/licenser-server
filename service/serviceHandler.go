package service

import (
	"net/http"

	"github.com/labstack/echo/v4"
)

type tempStruct struct {
	HarewareInfo string `json:"hardwareInfo"`
	Hash         string `json:"hash"`
}

//Gethwinfo ...
func Gethwinfo(c echo.Context) (err error) {
	defer func() {
		if err != nil {
			err = c.JSON(http.StatusBadRequest, err.Error())
			return
		}
	}()
	hwInfo := Gethardware()
	hashInfo, _ := HashInfo()
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, &tempStruct{HarewareInfo: hwInfo, Hash: hashInfo})

}
