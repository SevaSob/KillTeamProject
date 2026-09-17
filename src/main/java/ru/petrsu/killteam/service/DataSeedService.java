package ru.petrsu.killteam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.petrsu.killteam.entity.*;
import ru.petrsu.killteam.repository.*;

@Service
@RequiredArgsConstructor
public class DataSeedService {

    private final FactionRepository factionRepo;
    private final KillTeamRepository killTeamRepo;
    private final OperativeRepository operativeRepo;
    private final MatchRepository matchRepo;
    private final GameEventRepository eventRepo;
    private final JdbcTemplate jdbc;

    @Transactional
    public void resetAndSeed() {
        wipe();
        seed();
    }

    @Transactional
    public void seedIfEmpty() {
        if (factionRepo.count() == 0) seed();
    }

    public boolean isEmpty() {
        return factionRepo.count() == 0;
    }

    private void wipe() {
        jdbc.execute("TRUNCATE TABLE " +
                "game_event, turning_point, match_player, match_operative, match_game, " +
                "weapon, operative, kill_team, faction " +
                "RESTART IDENTITY CASCADE");
        System.out.println(">>> Таблицы очищены.");
    }

    private void seed() {
        // ==================== ФРАКЦИИ ====================
        Faction imperium = new Faction("Империум", "Силы Империума Человечества");
        Faction chaos = new Faction("Хаос", "Силы Хаоса и предатели");
        Faction xenos = new Faction("Ксеносы", "Инопланетные расы");
        factionRepo.save(imperium);
        factionRepo.save(chaos);
        factionRepo.save(xenos);

        Faction astartes = makeChild("Адептус Астартес",
                "Космические десантники Императора", imperium);
        Faction deathGuard = makeChild("Гвардия Смерти",
                "Легион Нургла, медленные, живучие, ядовитые", chaos);
        Faction tau = makeChild("Империя Тау",
                "Технологичная молодая раса, сильна на дистанции", xenos);
        Faction orks = makeChild("Орки",
                "Зеленокожие воины, ярость и числа", xenos);
        Faction necrons = makeChild("Некроны",
                "Древние машины-скелеты, восстанавливаются после смерти", xenos);
        Faction aeldari = makeChild("Аэльдари",
                "Древняя раса, быстрые и хрупкие, владеют пси-силами", xenos);

        System.out.println(">>> Фракции созданы: " + factionRepo.count());

        // ==================== SPACE MARINE ====================
        KillTeam sm = new KillTeam(astartes, "Космодесант", "2024.1",
                "Элитные воины Императора. Тяжёлая броня, мощное оружие, "
                        + "сильны и в стрельбе, и в ближнем бою.");
        killTeamRepo.save(sm);

        Operative smCaptain = op("Капитан Космодесанта", 3, 6, "3+", 14,
                "Железный Ореол: 4+ неуязвимый спас-бросок. "
                        + "Обряды Битвы: перебрасывай 1 при стрельбе.");
        smCaptain.addWeapon(w("Мастерский болтер", 4, "3+", "3/4", "Дальность 24\""));
        smCaptain.addWeapon(w("Силовой меч", 5, "3+", "4/5", "Смертельное 5+"));
        saveOp(smCaptain, sm);

        Operative smIntercessor = op("Интерцессор", 3, 6, "3+", 12,
                "Болтерная дисциплина: болтерное оружие получает Смертельное 5+, если не двигался.");
        smIntercessor.addWeapon(w("Болтер-винтовка", 4, "3+", "3/4", "Дальность 24\", Смертельное 5+"));
        smIntercessor.addWeapon(w("Ближний бой", 4, "3+", "3/4", ""));
        saveOp(smIntercessor, sm);

        Operative smAssault = op("Штурмовой Интерцессор", 3, 6, "3+", 12,
                "Ударная атака: +1 атака при заряде.");
        smAssault.addWeapon(w("Тяжёлый болт-пистолет", 4, "3+", "3/4", "Дальность 18\""));
        smAssault.addWeapon(w("Астартес-цепной меч", 5, "3+", "3/4", "Смертельное 5+"));
        saveOp(smAssault, sm);

        Operative smHeavy = op("Тяжёлый Интерцессор", 2, 5, "3+", 15,
                "Непреклонность: снижай получаемый урон на 1 (минимум 1).");
        smHeavy.addWeapon(w("Тяжёлая болтер-винтовка", 5, "3+", "3/4", "Дальность 30\", Пробитие 1"));
        saveOp(smHeavy, sm);

        Operative smSniper = op("Снайпер-Элиминатор", 3, 6, "3+", 12,
                "Маскировочный плащ: нельзя выбрать целью, если в укрытии и дальше 6\".");
        smSniper.addWeapon(w("Лаз-фузиль", 4, "2+", "4/6", "Дальность 36\", Пробитие 1, Тяжёлое"));
        smSniper.addWeapon(w("Ближний бой", 3, "3+", "3/4", ""));
        saveOp(smSniper, sm);

        Operative smGren = op("Гренадёр", 3, 6, "3+", 12,
                "Подрывник: перебрасывай урон гранат.");
        smGren.addWeapon(w("Болтер", 4, "3+", "3/4", "Дальность 24\""));
        smGren.addWeapon(w("Осколочная граната", 4, "3+", "3/5", "Взрыв 2\", Дальность 6\""));
        saveOp(smGren, sm);

        System.out.println(">>> Космодесант: 6 бойцов");

        // ==================== GREY KNIGHT ====================
        KillTeam gk = new KillTeam(imperium, "Серые Рыцари", "2024.1",
                "Пси-воины, охотники на демонов. Владеют телепатией и силой разума.");
        killTeamRepo.save(gk);

        Operative gkJust = op("Юстикар Серых Рыцарей", 3, 6, "3+", 14,
                "Кары: пси-атака. Молот-рука: +1 урон в ближнем бою.");
        gkJust.addWeapon(w("Штормовой болтер", 4, "3+", "3/4", "Дальность 24\""));
        gkJust.addWeapon(w("Немезидский силовой меч", 5, "3+", "4/6", "Смертельное 5+"));
        gkJust.addWeapon(w("Кары", 5, "3+", "2/3", "Психическая, Дальность 12\""));
        saveOp(gkJust, gk);

        Operative gkStrike = op("Ударный Серый Рыцарь", 3, 6, "3+", 12,
                "Телепорт-удар: глубокое развёртывание.");
        gkStrike.addWeapon(w("Штормовой болтер", 4, "3+", "3/4", "Дальность 24\""));
        gkStrike.addWeapon(w("Немезидский алебард", 5, "3+", "4/5", ""));
        saveOp(gkStrike, gk);

        Operative gkTerm = op("Терминатор Серых Рыцарей", 3, 5, "2+", 18,
                "Крест Терминатус: 5+ неуязвимый спас-бросок.");
        gkTerm.addWeapon(w("Штормовой болтер", 4, "3+", "3/4", "Дальность 24\""));
        gkTerm.addWeapon(w("Немезидский силовой меч", 5, "3+", "4/6", "Смертельное 5+"));
        saveOp(gkTerm, gk);

        Operative gkInt = op("Перехватчик Серых Рыцарей", 3, 8, "3+", 12,
                "Личный телепорт: может двигаться через террейн.");
        gkInt.addWeapon(w("Штормовой болтер", 4, "3+", "3/4", "Дальность 24\""));
        gkInt.addWeapon(w("Немезидский силовой меч", 4, "3+", "4/5", ""));
        saveOp(gkInt, gk);

        Operative gkPur = op("Очиститель Серых Рыцарей", 3, 6, "3+", 12,
                "Очищающее пламя: огнемёт игнорирует укрытие.");
        gkPur.addWeapon(w("Испепелитель", 5, "3+", "4/5", "Дальность 12\", Поток 6\""));
        gkPur.addWeapon(w("Немезидский силовой меч", 4, "3+", "4/5", ""));
        saveOp(gkPur, gk);

        // ==================== PLAGUE MARINES ====================
        KillTeam dg = new KillTeam(deathGuard, "Чумные Десантники", "2025.1",
                "Медленные, но невероятно живучие. Яды, токсины, гниль. "
                        + "Благословение Дедушки лечит раны.");
        killTeamRepo.save(dg);

        Operative dgChamp = op("Чемпион Чумных Десантников", 3, 5, "3+", 15,
                "Благословение Дедушки: лечит до 3 ран за раунд, если рядом враг с Ядом.");
        dgChamp.addWeapon(w("Плазменный пистолет (обычный)", 4, "3+", "3/5", "Дальность 8\", Пробитие 1"));
        dgChamp.addWeapon(w("Плазменный пистолет (перезаряд)", 4, "3+", "4/5", "Дальность 8\", Горячий, Смертельное 5+, Пробитие 1"));
        dgChamp.addWeapon(w("Чумной меч", 5, "3+", "4/5", "Суровое, Яд, Токсин"));
        saveOp(dgChamp, dg);

        Operative dgWar = op("Воин Чумных Десантников", 3, 5, "3+", 14,
                "Токсин: +1 к урону, если у цели был Яд.");
        dgWar.addWeapon(w("Болтер", 4, "3+", "3/4", "Дальность 24\""));
        dgWar.addWeapon(w("Чумной нож", 4, "3+", "3/5", "Суровое, Яд"));
        saveOp(dgWar, dg);

        Operative dgFight = op("Боец Чумных Десантников", 3, 5, "3+", 14,
                "Токсин: +1 к урону, если у цели был Яд.");
        dgFight.addWeapon(w("Чумной цеп", 5, "3+", "4/5", "Суровое, Яд, Токсин"));
        saveOp(dgFight, dg);

        Operative dgHeavy = op("Тяжёлый стрелок Чумных Десантников", 3, 5, "3+", 15,
                "Токсин: +1 к урону, если у цели был Яд.");
        dgHeavy.addWeapon(w("Гнилостный гранатомёт", 5, "3+", "3/5", "Дальность 18\", Взрыв 3\", Яд"));
        saveOp(dgHeavy, dg);

        Operative dgIcon = op("Знаменосец Чумных Десантников", 3, 5, "3+", 14,
                "Токсин: +1 к урону, если у цели был Яд.");
        dgIcon.addWeapon(w("Болтер", 4, "3+", "3/4", "Дальность 24\""));
        dgIcon.addWeapon(w("Чумной нож", 4, "3+", "3/5", "Суровое, Яд"));
        saveOp(dgIcon, dg);

        Operative dgBlight = op("Терминатор Гнилостной Стражи", 3, 4, "2+", 18,
                "Отвратительная стойкость: 5+ игнорировать урон.");
        dgBlight.addWeapon(w("Гнилостный гранатомёт", 5, "3+", "3/5", "Дальность 18\", Взрыв 3\", Яд"));
        dgBlight.addWeapon(w("Буботический топор", 5, "3+", "4/6", "Яд, Токсин"));
        saveOp(dgBlight, dg);

        Operative dgTally = op("Счётчик", 3, 5, "3+", 13,
                "Счёт Чумы: может дать +1 КО при броске 6+.");
        dgTally.addWeapon(w("Плазменный пистолет", 4, "3+", "3/5", "Дальность 8\", Пробитие 1"));
        dgTally.addWeapon(w("Чумной нож", 4, "3+", "3/5", "Суровое, Яд"));
        saveOp(dgTally, dg);

        // ==================== TRAITOR SPACE MARINES ====================
        KillTeam csm = new KillTeam(chaos, "Предатели-Космодесантники", "2024.1",
                "Предатели, служащие тёмным богам. Ярость и хаос, сильны в ближнем бою.");
        killTeamRepo.save(csm);

        Operative csmChamp = op("Чемпион Предателей", 3, 6, "3+", 14,
                "Тёмный Фанатизм: перебрасывай 1 при попадании.");
        csmChamp.addWeapon(w("Болт-пистолет", 4, "3+", "3/4", "Дальность 12\""));
        csmChamp.addWeapon(w("Силовой топор", 5, "3+", "4/6", "Смертельное 5+"));
        saveOp(csmChamp, csm);

        Operative csmWar = op("Космодесантник-Предатель", 3, 6, "3+", 14,
                "Пусть Галактика Горит: +1 атака при заряде.");
        csmWar.addWeapon(w("Болтер", 4, "3+", "3/4", "Дальность 24\""));
        csmWar.addWeapon(w("Цепной меч", 5, "3+", "3/4", "Смертельное 5+"));
        saveOp(csmWar, csm);

        Operative csmBers = op("Берсерк Кхорна", 3, 7, "3+", 14,
                "Кровь для Бога Крови: +2 атаки при заряде.");
        csmBers.addWeapon(w("Болт-пистолет", 4, "3+", "3/4", "Дальность 12\""));
        csmBers.addWeapon(w("Цепной топор", 6, "3+", "4/5", "Смертельное 5+"));
        saveOp(csmBers, csm);

        Operative csmHavoc = op("Опустошитель", 2, 5, "3+", 14,
                "Демоническая Кузня: тяжёлое оружие не получает -1 при движении.");
        csmHavoc.addWeapon(w("Жнец-пушка", 6, "3+", "3/5", "Дальность 30\", Тяжёлое"));
        csmHavoc.addWeapon(w("Ближний бой", 3, "3+", "3/4", ""));
        saveOp(csmHavoc, csm);

        Operative csmTerm = op("Терминатор Хаоса", 3, 5, "2+", 18,
                "Крест Терминатус: 5+ неуязвимый спас-бросок.");
        csmTerm.addWeapon(w("Комби-болтер", 4, "3+", "3/4", "Дальность 24\""));
        csmTerm.addWeapon(w("Силовой кулак", 4, "3+", "5/6", ""));
        saveOp(csmTerm, csm);

        Operative csmSorc = op("Колдун", 3, 6, "3+", 14,
                "Предвидение: союзники в 6\" перебрасывают 1.");
        csmSorc.addWeapon(w("Силовой посох", 4, "3+", "4/6", "Психическая"));
        csmSorc.addWeapon(w("Кары", 5, "3+", "2/3", "Психическая, Дальность 12\""));
        saveOp(csmSorc, csm);

        // ==================== T'AU ====================
        KillTeam tauTeam = new KillTeam(tau, "Кадр Охотников", "2024.1",
                "Кадр охотников Тау. Дальний бой, дроны, технологии. Слабы в ближнем бою.");
        killTeamRepo.save(tauTeam);

        Operative tauBlade = op("Командир Кадра", 3, 6, "4+", 12,
                "Залповый огонь: +1 выстрел, если цель в 12\".");
        tauBlade.addWeapon(w("Импульсная винтовка", 4, "3+", "3/4", "Дальность 30\""));
        saveOp(tauBlade, tauTeam);

        Operative tauShas = op("Воин Огня Шас'ла", 2, 6, "4+", 10,
                "Маркерный свет: союзники получают +1 к попаданию по цели.");
        tauShas.addWeapon(w("Импульсная винтовка", 4, "3+", "3/4", "Дальность 30\""));
        tauShas.addWeapon(w("Импульсный пистолет", 3, "3+", "3/4", "Дальность 12\""));
        saveOp(tauShas, tauTeam);

        Operative tauPath = op("Следопыт", 2, 7, "5+", 9,
                "Передовой наблюдатель: может наводить союзников на цель.");
        tauPath.addWeapon(w("Импульсный карабин", 3, "3+", "3/4", "Дальность 18\""));
        tauPath.addWeapon(w("Маркерный свет", 1, "3+", "0/0", "Дальность 36\", Маркер"));
        saveOp(tauPath, tauTeam);

        Operative tauStealth = op("Стелс-скафандр", 3, 8, "3+", 12,
                "Поле скрытности: нельзя выбрать целью дальше 12\".");
        tauStealth.addWeapon(w("Импульсная пушка", 5, "3+", "3/5", "Дальность 24\""));
        saveOp(tauStealth, tauTeam);

        Operative tauCrisis = op("Кризис-скафандр", 3, 8, "3+", 14,
                "Реактивный ранец: можно двигаться после стрельбы.");
        tauCrisis.addWeapon(w("Плазменная винтовка", 4, "3+", "4/6", "Дальность 24\", Пробитие 1"));
        tauCrisis.addWeapon(w("Фузионный бластер", 4, "3+", "5/6", "Дальность 12\", Мельта"));
        saveOp(tauCrisis, tauTeam);

        Operative tauDrone = op("Дрон-стрелок", 2, 8, "4+", 8,
                "Дрон: можно выбрать целью только если он ближайший.");
        tauDrone.addWeapon(w("Спаренные импульсные карабины", 4, "3+", "3/4", "Дальность 18\""));
        saveOp(tauDrone, tauTeam);

        // ==================== ORKS ====================
        KillTeam orkTeam = new KillTeam(orks, "Зеленокожие", "2024.1",
                "Много, дёшево, яростно. Сильны в ближнем бою.");
        killTeamRepo.save(orkTeam);

        Operative orkNob = op("Ноб", 3, 6, "4+", 12,
                "Погнали: +1 атака при заряде.");
        orkNob.addWeapon(w("Слагга", 3, "3+", "3/4", "Дальность 12\""));
        orkNob.addWeapon(w("Большой рубач", 5, "3+", "4/6", ""));
        saveOp(orkNob, orkTeam);

        Operative orkBoy = op("Орк-парень", 2, 6, "6+", 8,
                "Правило толпы: +1 к лидерству за каждых 5 орков рядом.");
        orkBoy.addWeapon(w("Слагга", 3, "3+", "3/4", "Дальность 12\""));
        orkBoy.addWeapon(w("Рубач", 4, "3+", "3/5", ""));
        saveOp(orkBoy, orkTeam);

        Operative orkShoota = op("Орк-стрелок", 2, 6, "6+", 8,
                "Дака-дака-дака: 6 на попадание даёт дополнительные выстрелы.");
        orkShoota.addWeapon(w("Шута", 4, "3+", "3/4", "Дальность 18\", Штурмовое"));
        orkShoota.addWeapon(w("Рубач", 3, "3+", "3/5", ""));
        saveOp(orkShoota, orkTeam);

        Operative orkRokk = op("Орк с ракетницей", 2, 6, "6+", 8,
                "Ракета: взрывной урон.");
        orkRokk.addWeapon(w("Ракетница", 4, "3+", "5/6", "Дальность 24\", Взрыв 2\""));
        orkRokk.addWeapon(w("Рубач", 3, "3+", "3/5", ""));
        saveOp(orkRokk, orkTeam);

        Operative orkBurna = op("Орк-поджигатель", 2, 6, "6+", 8,
                "Бурна: поток, игнорирует укрытие.");
        orkBurna.addWeapon(w("Бурна", 4, "3+", "4/5", "Дальность 8\", Поток 6\""));
        orkBurna.addWeapon(w("Рубач", 3, "3+", "3/5", ""));
        saveOp(orkBurna, orkTeam);

        Operative orkKomm = op("Коммандо", 3, 7, "6+", 9,
                "Инфильтрация: развёртывание вне зоны противника.");
        orkKomm.addWeapon(w("Слагга", 3, "3+", "3/4", "Дальность 12\""));
        orkKomm.addWeapon(w("Рубач", 4, "3+", "3/5", ""));
        saveOp(orkKomm, orkTeam);

        // ==================== NECRONS ====================
        KillTeam necTeam = new KillTeam(necrons, "Миры Гробниц", "2024.1",
                "Древние машины-скелеты. Протоколы Восстановления возвращают убитых.");
        killTeamRepo.save(necTeam);

        Operative necLord = op("Лорд Некронов", 3, 5, "3+", 14,
                "Сфера Воскрешения: восстановление на 4+.");
        necLord.addWeapon(w("Посох света", 5, "3+", "4/6", "Дальность 18\", Пробитие 1"));
        necLord.addWeapon(w("Ближний бой", 4, "3+", "4/5", ""));
        saveOp(necLord, necTeam);

        Operative necWar = op("Воин Некронов", 2, 5, "4+", 10,
                "Протоколы Восстановления: 5+ игнорировать урон.");
        necWar.addWeapon(w("Гаусс-излучатель", 4, "3+", "3/5", "Дальность 24\", Пробитие 1"));
        saveOp(necWar, necTeam);

        Operative necImm = op("Бессмертный", 3, 5, "3+", 12,
                "Протоколы Восстановления: 5+ игнорировать урон.");
        necImm.addWeapon(w("Гаусс-бластер", 5, "3+", "4/6", "Дальность 24\", Пробитие 1"));
        saveOp(necImm, necTeam);

        Operative necDeath = op("Метка Смерти", 3, 6, "3+", 12,
                "Охотники из Гиперпространства: глубокое развёртывание.");
        necDeath.addWeapon(w("Синаптический дезинтегратор", 4, "2+", "4/6", "Дальность 36\", Пробитие 1"));
        saveOp(necDeath, necTeam);

        Operative necLych = op("Лич-страж", 3, 5, "2+", 16,
                "Дисперсионный щит: 4+ неуязвимый спас-бросок.");
        necLych.addWeapon(w("Боевая коса", 5, "3+", "5/6", ""));
        saveOp(necLych, necTeam);

        Operative necScar = op("Рой Скарабеев", 2, 8, "6+", 6,
                "Рой: может двигаться через любой террейн.");
        necScar.addWeapon(w("Жвалы-кормильцы", 5, "4+", "2/3", ""));
        saveOp(necScar, necTeam);

        // ==================== AELDARI ====================
        KillTeam aelTeam = new KillTeam(aeldari, "Мир-Корабль", "2024.1",
                "Древняя раса. Быстрые, точные, хрупкие. Мастера пси-сил.");
        killTeamRepo.save(aelTeam);

        Operative aelWarlock = op("Чернокнижник", 3, 7, "4+", 10,
                "Руны Битвы: пси-силы усиливают союзников.");
        aelWarlock.addWeapon(w("Ведьмин клинок", 4, "3+", "4/6", "Психическая"));
        aelWarlock.addWeapon(w("Шурикен-пистолет", 3, "3+", "3/4", "Дальность 12\", Рвущее"));
        saveOp(aelWarlock, aelTeam);

        Operative aelAvenger = op("Мститель", 3, 7, "4+", 9,
                "Боевой Фокус: можно стрелять после рывка.");
        aelAvenger.addWeapon(w("Шурикен-катапульта Мстителя", 4, "3+", "3/4", "Дальность 18\", Рвущее"));
        saveOp(aelAvenger, aelTeam);

        Operative aelRanger = op("Рейнджер", 3, 8, "5+", 8,
                "Маскировочный плащ: нельзя выбрать целью дальше 12\".");
        aelRanger.addWeapon(w("Длинная винтовка Рейнджера", 4, "2+", "4/5", "Дальность 36\", Тяжёлое, Рвущее"));
        saveOp(aelRanger, aelTeam);

        Operative aelBanshee = op("Воющая Банши", 3, 8, "5+", 9,
                "Маска Банши: враг не может стрелять в ответ.");
        aelBanshee.addWeapon(w("Силовой меч", 5, "3+", "4/5", ""));
        aelBanshee.addWeapon(w("Шурикен-пистолет", 3, "3+", "3/4", "Дальность 12\", Рвущее"));
        saveOp(aelBanshee, aelTeam);

        Operative aelWraith = op("Клинок-Призрак", 3, 5, "3+", 16,
                "Призрачная конструкция: игнорирует боль.");
        aelWraith.addWeapon(w("Меч-призрак", 5, "3+", "5/6", ""));
        saveOp(aelWraith, aelTeam);

        System.out.println("========================================");
        System.out.println(">>> БАЗА ЗАПОЛНЕНА:");
        System.out.println(">>> Фракций: " + factionRepo.count());
        System.out.println(">>> Команд: " + killTeamRepo.count());
        System.out.println(">>> Оперативников: " + operativeRepo.count());
        System.out.println("========================================");

        // ==================== DEMO-МАТЧ ====================
        if (matchRepo.count() == 0) {
            Match match = new Match("Демо-бой", "Добыча", "30x22");
            MatchPlayer p1 = new MatchPlayer("Игрок 1", sm);
            MatchPlayer p2 = new MatchPlayer("Игрок 2", dg);
            p1.setInitiative(true);
            match.addPlayer(p1);
            match.addPlayer(p2);
            for (int i = 1; i <= 4; i++) match.addTurningPoint(new TurningPoint(i));
            matchRepo.save(match);

            GameEvent e1 = new GameEvent("NOTE", "Матч создан. Инициатива у Игрока 1.");
            e1.setMatch(match); e1.setPlayer(p1); eventRepo.save(e1);

            GameEvent e2 = new GameEvent("CP_GAIN", "Игрок 1 получает +1 КО за инициативу.");
            e2.setMatch(match); e2.setPlayer(p1); eventRepo.save(e2);

            GameEvent e3 = new GameEvent("CP_GAIN", "Игрок 2 получает +2 КО.");
            e3.setMatch(match); e3.setPlayer(p2); eventRepo.save(e3);

            System.out.println(">>> Демо-матч создан: id=" + match.getId());
        }
    }

    // ==================== ХЕЛПЕРЫ ====================
    private Faction makeChild(String name, String desc, Faction parent) {
        Faction f = new Faction(name, desc);
        f.setParent(parent);
        return factionRepo.save(f);
    }

    private Operative op(String name, int apl, int move, String save, int wounds, String ability) {
        return new Operative(name, apl, move, save, wounds, ability);
    }

    private Weapon w(String name, int atk, String hit, String dmg, String special) {
        return new Weapon(name, atk, hit, dmg, special);
    }

    private void saveOp(Operative o, KillTeam team) {
        o.setKillTeam(team);
        operativeRepo.save(o);
    }
}