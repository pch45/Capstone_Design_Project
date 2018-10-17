#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#include <pthread.h> 
#include "getch.h"

//#define DAY_H 24
#define _CRT_SECURE_NO_WARNINGS
#define HOUR_M 60
#define MIN_S 60
#define PERIOD 10 // unit second
#define MAX ((HOUR_M * MIN_S) / PERIOD)
#define CURRENT_TIMEZONE_OFFSET +9

struct CQA {
	int hour;
	int min;
	int sec;
	int data;
};

struct CQA cqa[MAX];
int max = 5;
int count = 0;
int num = 0;
int check = 0;

int thr_id1, thr_id2;

pthread_t p_thread[2];

//int status;
int a = 1;
int b = 2;
int c = 3;
//void *tret;

void cqa_insert(int value);
struct CQA cqa_delete();
int peek();
int isEmpty();
int cqa_size();
void cqa_insert(int value);
void data_collect();
int transmit();

void cqa_insert(int value)
{
	if (num == max) {
		time_t timer;
		struct tm *t;

		timer = time(NULL); // 현재 시각을 초 단위로 얻기
		timer -= CURRENT_TIMEZONE_OFFSET * 3600;
		timer += 9 * 3600;
		t = localtime(&timer); // 초 단위의 시간을 분리하여 구조체에 넣기

		cqa[count % MAX].hour = t->tm_hour;
		cqa[count % MAX].min = t->tm_min;
		cqa[count % MAX].sec = t->tm_sec;
		cqa[count % MAX].data = value;

		count++;
	}
	else {
		time_t timer;
		struct tm *t;

		timer = time(NULL); // 현재 시각을 초 단위로 얻기
		timer -= CURRENT_TIMEZONE_OFFSET * 3600;
		timer += 9 * 3600;
		t = localtime(&timer); // 초 단위의 시간을 분리하여 구조체에 넣기

		cqa[count % MAX].hour = t->tm_hour;
		cqa[count % MAX].min = t->tm_min;
		cqa[count % MAX].sec = t->tm_sec;
		cqa[count % MAX].data = value;

		count++;
		num++;
	}
}

struct CQA cqa_delete() {
	struct CQA temp = cqa[(count - 1) % MAX];
	cqa[(count - 1) % MAX].data = -999;
	cqa[(count - 1) % MAX].hour = -999;
	cqa[(count - 1) % MAX].min = -999;
	cqa[(count - 1) % MAX].sec = -999;
	num--;
    count--;
	return temp;
}

int peek() {
	if (cqa[(count - 1) % MAX].data != -999) {
		return 1;
	}
	else {
		return -1;
	}
}

int isEmpty() {
	if (num > 0) {
		return 1;
	}
	else {
		return -1;
	}
}

void data_collect() {
	int dustPCS = 0;
	float dustValue = 0;
	int fd;

	char send[] = { 0x11, 0x02, 0x0B, 0x01, 0xE1 };
	char response[7];
	printf("start \n");
	while (1) {
		fd = serialOpen("/dev/ttyAMA0", 9600);
		write(fd, send, sizeof(send));
		read(fd, response, sizeof(response));
		if (response[0] == 0x16) {
			dustPCS = response[3] * 256 * 256 * 256 + response[4] * 256 * 256 + response[5] * 256 + response[6];
			dustValue = ((float)(dustPCS * 3528)) / 100000;
			printf("Now dust : %d \n", (int)(dustValue * 100000) / 3528);
			cqa_insert((int)(dustValue * 100000) / 3528);
			delay(2000);
		}
		else if (response[0] == 0x06) {
			printf("Fail to Receive \n");
			delay(2000);
			continue;
		}
		serialClose(fd);
	}
	printf("End \n");
}

int transmit() {
	system("sudo hciconfig hci0 up");
	while (isEmpty() != -1 && peek() != -1) {
		struct CQA temp = cqa_delete();
		char buf[256];
		system("sudo hciconfig hci0 leadv 3");
		system("sudo hciconfig hci0 noscan");
		printf("%d %d %d %d\n", temp.hour, temp.min, temp.sec, temp.data);
		sprintf(buf, "sudo hcitool -i hci0 cmd 0x08 0x0008 1E 02 01 %X %X %X 4C 00 02 %X E2 0A 39 F4 73 F5 4B C4 A1 2F 17 D1 AD 07 A9 61 00 01 00 01 C8 00", temp.hour, temp.min, temp.sec, temp.data);
		system(buf);
		delay(1000);
		system("sudo hciconfig hci0 noleadv");
		delay(5000);
	}
	system("sudo hciconfig hci0 leadv 3");
	system("sudo hciconfig hci0 noscan");
	system("sudo hcitool -i hci0 cmd 0x08 0x0008 1E 02 01 FF FF FF 4C 00 02 FF E2 0A 39 F4 73 F5 4B C4 A1 2F 17 D1 AD 07 A9 61 00 01 00 01 C8 00");
	delay(1000);
	system("sudo hciconfig hci0 noleadv");
	delay(1000);

    thr_id1 = pthread_create(&p_thread[0], NULL, data_collect, (void *)&a);
    check = 1;

	return 1;
}

/*void key_insert() {
	
}*/

int main() {

//	int thr_id1, thr_id2;

//	pthread_t p_thread[2];

//	int status;
//	int a = 1;
//	int b = 2;

	//데이터 수집
	thr_id1 = pthread_create(&p_thread[0], NULL, data_collect, (void *)&a);
	//키 입력
	while(1) {
		char ch_ = 0;
		ch_ = getch();
		if (ch_ != NULL) {
			//pthread_exit((void *)2);
			//break;
			pthread_cancel(p_thread[0]);
			thr_id2 = pthread_create(&p_thread[1], NULL, transmit, (void *)&b);

			while (1) {
				char ch_1 = 0;
				ch_1 = getch();
				if (ch_1 != NULL && check != 1) {
					//transmit exit
					pthread_cancel(p_thread[1]);
					system("sudo hciconfig hci0 leadv 3");
					system("sudo hciconfig hci0 noscan");
					system("sudoz hcitool -i hci0 cmd 0x08 0x0008 1E 02 01 FF FF FF 4C 00 02 FF E2 0A 39 F4 73 F5 4B C4 A1 2F 17 D1 AD 07 A9 61 00 01 00 01 C8 00");
					delay(1000);
					system("sudo hciconfig hci0 noleadv");
					delay(1000);
					thr_id1 = pthread_create(&p_thread[0], NULL, data_collect, (void *)&a);
					ch_ = 0;
					break;
				}
                else {
                    check = 0;
                    break;
                }
			}
		}
	}

}
